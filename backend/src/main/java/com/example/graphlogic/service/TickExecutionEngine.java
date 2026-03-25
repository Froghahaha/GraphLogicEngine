
package com.example.graphlogic.service;

import com.example.graphlogic.model.Edge;
import com.example.graphlogic.model.Flowchart;
import com.example.graphlogic.model.Node;
import com.example.graphlogic.model.NodeStatus;
import com.example.graphlogic.capability.*;
import com.example.graphlogic.compiler.GraphCompiler;
import com.example.graphlogic.vm.BytecodeVm;
import com.example.graphlogic.vm.HostBinding;
import com.example.graphlogic.vm.Program;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TickExecutionEngine - 真正的 Tick 驱动执行引擎
 * 
 * 核心原则：
 * 1. 每个 Tick 是一个原子执行单元
 * 2. 所有状态变更在 Tick 结束时提交
 * 3. 同一 Tick 内所有逻辑看到的是一致的快照
 * 4. 不搞过度抽象，直接用 Map 存状态
 */
public class TickExecutionEngine {

    private final SimpMessagingTemplate messagingTemplate;
    private final long tickMillis;
    private final CapabilityRegistry capabilityRegistry;
    private final Map<String, CapabilityInstance> runningCapabilities = new ConcurrentHashMap<>();

    // 节点状态：INACTIVE, ACTIVE, WAITING, COMPLETED, ERROR
    private final Map<String, String> nodeRuntimeStatus = new ConcurrentHashMap<>();

    // 外部变量快照（双缓冲：current 用于读，next 用于写）
    private final Map<String, Object> externalVarsCurrent = new ConcurrentHashMap<>();
    private final Map<String, Object> externalVarsNext = new ConcurrentHashMap<>();

    // 内部变量快照（双缓冲）
    private final Map<String, Object> internalVarsCurrent = new ConcurrentHashMap<>();
    private final Map<String, Object> internalVarsNext = new ConcurrentHashMap<>();

    public TickExecutionEngine(SimpMessagingTemplate messagingTemplate, long tickMillis, CapabilityRegistry capabilityRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.tickMillis = tickMillis;
        this.capabilityRegistry = capabilityRegistry;
        // 初始化默认外部变量
        externalVarsCurrent.put("WorkpieceExist", false);
        externalVarsCurrent.put("sensor_start", false);
        externalVarsCurrent.put("sensor_stop", false);
        // 初始化 Next 缓冲区
        externalVarsNext.putAll(externalVarsCurrent);
    }

    /**
     * 设置外部变量值（写入 Next 缓冲区，下一个 Tick 生效）
     */
    public void setExternalVariable(String name, Object value) {
        externalVarsNext.put(name, value);
    }

    /**
     * 获取外部变量值（从 Current 缓冲区读取）
     */
    public Object getExternalVariable(String name) {
        return externalVarsCurrent.get(name);
    }

    /**
     * Tick-driven execution loop - 真正的 Tick 驱动模型
     * 
     * 核心原则：
     * 1. 每个 Tick 是一个原子执行单元
     * 2. 所有状态变更在 Tick 结束时提交
     * 3. 同一 Tick 内所有逻辑看到的是一致的快照
     * 4. 不搞过度抽象，直接用 Map 存状态
     */
    public void execute(Flowchart flowchart) {
        Program program = new GraphCompiler().compile(flowchart);

        final class EngineHost implements HostBinding {
            private final Map<Integer, CapabilityInstance> handles = new HashMap<>();
            private int nextHandle = 1;
            private Map<String, Object> extSnapshot = Map.of();

            void setExtSnapshot(Map<String, Object> snapshot) {
                if (snapshot == null) throw new IllegalArgumentException("snapshot is null");
                this.extSnapshot = snapshot;
            }

            @Override
            public long nowMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public Object readExternal(String symbolName) {
                if (symbolName == null || symbolName.isEmpty()) throw new IllegalArgumentException("symbolName is empty");
                return extSnapshot.get(symbolName);
            }

            @Override
            public int emitAction(String actionId, Map<String, Object> params) {
                if (actionId == null || actionId.isEmpty()) throw new IllegalArgumentException("actionId is empty");
                if (params == null) throw new IllegalArgumentException("params is null");

                CapabilityInstance instance = capabilityRegistry.get(actionId);
                if (instance == null) {
                    throw new IllegalStateException("Capability not found: " + actionId);
                }
                if (handles.containsValue(instance)) {
                    throw new IllegalStateException("Capability is not re-entrant: " + actionId);
                }

                long now = nowMillis();
                instance.start(params, now);

                int handle = nextHandle++;
                handles.put(handle, instance);
                return handle;
            }

            @Override
            public ActionPoll pollAction(int handle) {
                CapabilityInstance instance = handles.get(handle);
                if (instance == null) {
                    throw new IllegalStateException("Unknown action handle: " + handle);
                }

                long now = nowMillis();
                instance.tick(now);

                CapabilityLifecycleState state = instance.getLifecycleState();
                if (state == CapabilityLifecycleState.RUNNING) {
                    return new ActionPoll(false, 0);
                }
                if (state == CapabilityLifecycleState.DONE) {
                    handles.remove(handle);
                    return new ActionPoll(true, 0);
                }
                if (state == CapabilityLifecycleState.TIMEOUT) {
                    handles.remove(handle);
                    return new ActionPoll(true, 3);
                }
                if (state == CapabilityLifecycleState.ERROR) {
                    CapabilityResult result = instance.getResult();
                    int cat = 1;
                    if (result != null && result.getCategory() == CapabilityResultCategory.FATAL_ERROR) {
                        cat = 2;
                    }
                    handles.remove(handle);
                    return new ActionPoll(true, cat);
                }

                throw new IllegalStateException("Unsupported lifecycle state: " + state);
            }
        }

        EngineHost host = new EngineHost();
        BytecodeVm vm = new BytecodeVm(program, host, 2000, 0);

        String lastNodeId = null;
        while (!vm.halted()) {
            long tickStart = System.currentTimeMillis();

            externalVarsCurrent.clear();
            externalVarsCurrent.putAll(externalVarsNext);

            Map<String, Object> snapshot = new HashMap<>(externalVarsCurrent);
            host.setExtSnapshot(snapshot);

            vm.runTick();

            String nodeId = vm.lastNodeId();
            if (nodeId != null && !nodeId.equals(lastNodeId)) {
                lastNodeId = nodeId;
                updateNodeStatus(nodeId, "ACTIVE", "PC entered node");
            }

            long elapsed = System.currentTimeMillis() - tickStart;
            long remaining = tickMillis - elapsed;
            if (remaining > 0) {
                try {
                    Thread.sleep(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private Map<String, Node> mapNodes(Flowchart flowchart) {
        Map<String, Node> map = new HashMap<>();
        for (Node node : flowchart.getNodes()) {
            map.put(node.getId(), node);
        }
        return map;
    }

    private String findStartNodeId(Flowchart flowchart) {
        for (Node node : flowchart.getNodes()) {
            String type = node.getType();
            if (type == null) {
                continue;
            }
            String t = type.toLowerCase(Locale.ROOT);
            if ("start".equals(t) || "input".equals(t)) {
                return node.getId();
            }
        }
        return null;
    }

    /**
     * Select the next node based on node type, edge roles, and decision conditions.
     * Used for non-branching nodes or as a fallback.
     */
    private String selectNextNodeId(Node currentNode, Flowchart flowchart, Map<String, Object> externalSnapshot, Map<String, Object> internalSnapshot, Map<String, String> nodeStates) {
        String currentNodeId = currentNode.getId();
        List<Edge> outgoingEdges = flowchart.getEdges().stream()
                .filter(e -> e.getSource().equals(currentNodeId))
                .collect(Collectors.toList());

        if (outgoingEdges.isEmpty()) return null;

        // 只有一个出边，直接走
        if (outgoingEdges.size() == 1) {
            return outgoingEdges.get(0).getTarget();
        }

        // 多个出边但没明确类型处理，报错或选第一个（Linus 风格：要么明确，要么报错）
        updateNodeStatus(currentNodeId, "ERROR", "Multiple outgoing edges on non-branching node");
        return null;
    }

    // --- 移除冗余的 selectDecisionNextNode, selectTaskNextNode, selectJoinNextNode, selectDefaultNextNode ---

    /**
     * Evaluate decision condition based on external snapshot.
     * Currently supports:
     * - left.source = "external" with name
     * - op = EQ/NEQ
     * - right.literal (any value)
     */
    private boolean evalDecisionCondition(Node decisionNode, Map<String, Object> externalSnapshot, Map<String, Object> internalSnapshot, Map<String, String> nodeStates) {
        Map<String, Object> nodeData = decisionNode.getData();
        if (nodeData == null) {
            throw new RuntimeException("Decision node has no data");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> decisionData = (Map<String, Object>) nodeData.get("decision");
        if (decisionData == null) {
            throw new RuntimeException("Decision node has no decision data");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> condition = (Map<String, Object>) decisionData.get("condition");
        if (condition == null) {
            throw new RuntimeException("Decision node has no condition");
        }

        // Parse left side
        @SuppressWarnings("unchecked")
        Map<String, Object> left = (Map<String, Object>) condition.get("left");
        if (left == null) {
            throw new RuntimeException("Decision condition has no left side");
        }

        String source = (String) left.get("source");
        String name = (String) left.get("name");

        if (name == null || name.isEmpty()) {
            throw new RuntimeException("Variable name is empty");
        }

        // Parse operator
        String op = (String) condition.get("op");
        if (!"EQ".equals(op) && !"NEQ".equals(op)) {
            throw new RuntimeException("Only EQ and NEQ operators are supported, got: " + op);
        }

        // Parse right side
        @SuppressWarnings("unchecked")
        Map<String, Object> right = (Map<String, Object>) condition.get("right");
        if (right == null) {
            throw new RuntimeException("Decision condition has no right side");
        }

        Object literal = right.get("literal");
        if (literal == null) {
            throw new RuntimeException("Right side literal is null");
        }

        // Get variable value based on source
        Object varValue;
        if ("external".equals(source)) {
            varValue = externalSnapshot.get(name);
        } else if ("internal".equals(source)) {
            varValue = internalSnapshot.get(name);
        } else if ("taskResult".equals(source)) {
            varValue = nodeStates.get(name); // Basic support for task results
        } else {
            throw new RuntimeException("Unsupported variable source: " + source);
        }

        // If variable is missing, default to null handling or string "null"
        String varStr = varValue != null ? varValue.toString() : "null";
        String litStr = literal.toString();

        // Compare values
        boolean result;
        if ("EQ".equals(op)) {
            result = Objects.equals(varStr, litStr);
        } else { // NEQ
            result = !Objects.equals(varStr, litStr);
        }

        return result;
    }

    /**
     * 执行单个节点 - 状态流转权完全交给 executeNode
     */
    private void executeNode(Node node, Flowchart flowchart, Map<String, Node> nodeMap, 
                           Map<String, Object> externalVarsCurrent, Map<String, Object> internalVarsCurrent, Map<String, Object> internalVarsNext,
                           Set<String> nextActiveNodes, Map<String, String> nodeStatesCurrent, Map<String, String> nodeStatesNext) {
        String type = node.getType();
        String nodeId = node.getId();
        if (type == null) return;

        String t = type.toLowerCase(Locale.ROOT);
        String currentState = nodeStatesCurrent.getOrDefault(nodeId, "INACTIVE");

        // 更新当前执行状态到 WebSocket (可选，用于 UI 展示)
        updateNodeStatus(nodeId, "ACTIVE", "Executing " + t);

        if ("task".equals(t)) {
            if ("WAITING".equals(currentState)) {
                CapabilityInstance instance = runningCapabilities.get(nodeId);
                if (instance == null) {
                    nodeStatesNext.put(nodeId, "COMPLETED");
                    String nextId = selectNextNodeIdByRole(node, flowchart, "onSuccess");
                    if (nextId != null) nextActiveNodes.add(nextId);
                    return;
                }

                CapabilityLifecycleState state = instance.getLifecycleState();
                if (state == CapabilityLifecycleState.RUNNING) {
                    instance.tick(System.currentTimeMillis());
                    nodeStatesNext.put(nodeId, "WAITING");
                    nextActiveNodes.add(nodeId);
                } else {
                    // 任务结束，根据结果类别选择分支
                    String role = "onSuccess";
                    if (state == CapabilityLifecycleState.TIMEOUT) {
                        role = "onTimeout";
                    } else if (state == CapabilityLifecycleState.ERROR) {
                        CapabilityResult result = instance.getResult();
                        if (result != null && result.getCategory() == CapabilityResultCategory.FATAL_ERROR) {
                            role = "onAbort";
                        } else {
                            role = "onRetry";
                        }
                    }

                    nodeStatesNext.put(nodeId, state == CapabilityLifecycleState.DONE ? "COMPLETED" : "ERROR");
                    String nextId = selectNextNodeIdByRole(node, flowchart, role);
                    if (nextId != null) nextActiveNodes.add(nextId);
                    updateNodeStatus(nodeId, nodeStatesNext.get(nodeId), "Task finished with " + role);
                    runningCapabilities.remove(nodeId);
                }
            } else {
                // 首次进入：启动异步能力
                startCapability(node);
                nodeStatesNext.put(nodeId, "WAITING");
                nextActiveNodes.add(nodeId); // 下个 tick 开始轮询
                updateNodeStatus(nodeId, "WAITING", "Task started, waiting for capability...");
            }
        } else if ("decision".equals(t)) {
            // Decision 是瞬时节点
            try {
                boolean result = evalDecisionCondition(node, externalVarsCurrent, internalVarsCurrent, nodeStatesCurrent);
                String role = result ? "true" : "false";
                nodeStatesNext.put(nodeId, "COMPLETED");
                String nextId = selectNextNodeIdByRole(node, flowchart, role);
                if (nextId != null) {
                    nextActiveNodes.add(nextId);
                } else {
                    nodeStatesNext.put(nodeId, "ERROR");
                    updateNodeStatus(nodeId, "ERROR", "No edge for role: " + role);
                }
            } catch (Exception e) {
                nodeStatesNext.put(nodeId, "ERROR");
                updateNodeStatus(nodeId, "ERROR", "Eval failed: " + e.getMessage());
            }
        } else if ("join".equals(t)) {
            // Join 可能是跨 Tick 的
            if (checkJoinCondition(node, nodeStatesCurrent)) {
                nodeStatesNext.put(nodeId, "COMPLETED");
                String nextId = selectNextNodeIdByRole(node, flowchart, "onReady");
                if (nextId != null) nextActiveNodes.add(nextId);
            } else {
                // 还没凑齐，下个 tick 继续蹲守
                nodeStatesNext.put(nodeId, "WAITING");
                nextActiveNodes.add(nodeId);
            }
        } else if ("start".equals(t) || "input".equals(t) || "action".equals(t) || "default".equals(t)) {
            // 瞬时完成节点
            nodeStatesNext.put(nodeId, "COMPLETED");
            String nextId = selectNextNodeId(node, flowchart, externalVarsCurrent, internalVarsCurrent, nodeStatesCurrent);
            if (nextId != null) nextActiveNodes.add(nextId);
        } else if ("end".equals(t) || "output".equals(t)) {
            nodeStatesNext.put(nodeId, "COMPLETED");
            updateNodeStatus(nodeId, "COMPLETED", "Flow finished at end node");
        }
    }

    /**
     * 根据 role 选择下一个节点
     */
    private String selectNextNodeIdByRole(Node node, Flowchart flowchart, String targetRole) {
        List<Edge> outgoingEdges = flowchart.getEdges().stream()
                .filter(e -> e.getSource().equals(node.getId()))
                .collect(Collectors.toList());

        for (Edge edge : outgoingEdges) {
            Map<String, Object> data = edge.getData();
            if (data != null && targetRole.equals(data.get("role"))) {
                return edge.getTarget();
            }
        }
        return null;
    }

    /**
     * 启动能力 - 简单实现，后续可接入真实能力
     */
    private void startCapability(Node taskNode) {
        Map<String, Object> nodeData = taskNode.getData();
        if (nodeData == null) {
            throw new RuntimeException("Task node has no data");
        }
        
        String capabilityId = (String) nodeData.get("capabilityId");
        if (capabilityId == null || capabilityId.isEmpty()) {
            throw new RuntimeException("Task node missing capabilityId");
        }
        
        CapabilityInstance instance = capabilityRegistry.get(capabilityId);
        if (instance == null) {
            throw new RuntimeException("Capability not found: " + capabilityId);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) nodeData.get("capabilityParams");
        instance.start(params != null ? params : new HashMap<>(), System.currentTimeMillis());
        
        runningCapabilities.put(taskNode.getId(), instance);
    }

    /**
     * 检查 Join 节点的汇合条件
     */
    private boolean checkJoinCondition(Node joinNode, Map<String, String> nodeStates) {
        Map<String, Object> nodeData = joinNode.getData();
        if (nodeData == null) {
            return true; // 默认条件：立即放行
        }

        String mode = (String) nodeData.get("mode");
        if (mode == null) {
            return true; // 默认条件：立即放行
        }

        @SuppressWarnings("unchecked")
        List<String> taskIds = (List<String>) nodeData.get("taskIds");
        if (taskIds == null || taskIds.isEmpty()) {
            return true; // 没有指定任务，立即放行
        }

        if ("WaitAllDone".equals(mode)) {
            // 所有指定任务都处于 Done 状态
            for (String taskId : taskIds) {
                String state = nodeStates.get(taskId);
                if (!"COMPLETED".equals(state)) {
                    return false;
                }
            }
            return true;
        } else if ("WaitAnyDone".equals(mode)) {
            // 任意一个任务处于 Done 状态
            for (String taskId : taskIds) {
                String state = nodeStates.get(taskId);
                if ("COMPLETED".equals(state)) {
                    return true;
                }
            }
            return false;
        } else if ("WaitAllFinished".equals(mode)) {
            // 所有任务都处于终态 (Done/Error/Timeout)
            for (String taskId : taskIds) {
                String state = nodeStates.get(taskId);
                if (!"COMPLETED".equals(state) && !"ERROR".equals(state)) {
                    return false;
                }
            }
            return true;
        }

        return true; // 未知模式，默认放行
    }

    private void updateNodeStatus(String nodeId, String status, String message) {
        nodeRuntimeStatus.put(nodeId, status);
        NodeStatus nodeStatus = new NodeStatus(nodeId, status, message, System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/status", nodeStatus);
    }
}
