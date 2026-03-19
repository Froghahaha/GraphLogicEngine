
package com.example.graphlogic.service;

import com.example.graphlogic.model.Edge;
import com.example.graphlogic.model.Flowchart;
import com.example.graphlogic.model.Node;
import com.example.graphlogic.model.NodeStatus;
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

    // 节点状态：INACTIVE, ACTIVE, WAITING, COMPLETED, ERROR
    private final Map<String, String> nodeRuntimeStatus = new ConcurrentHashMap<>();

    // 外部变量快照（双缓冲：current 用于读，next 用于写）
    private final Map<String, Object> externalVarsCurrent = new ConcurrentHashMap<>();
    private final Map<String, Object> externalVarsNext = new ConcurrentHashMap<>();

    // 内部变量快照（双缓冲）
    private final Map<String, Object> internalVarsCurrent = new ConcurrentHashMap<>();
    private final Map<String, Object> internalVarsNext = new ConcurrentHashMap<>();

    public TickExecutionEngine(SimpMessagingTemplate messagingTemplate, long tickMillis) {
        this.messagingTemplate = messagingTemplate;
        this.tickMillis = tickMillis;
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
        // 初始化节点状态映射
        Map<String, String> nodeStates = new HashMap<>();
        for (Node node : flowchart.getNodes()) {
            nodeStates.put(node.getId(), "INACTIVE");
        }

        // 当前 Tick 中激活的节点集合
        Set<String> activeNodes = new HashSet<>();

        // 下一 Tick 中应该激活的节点集合
        Set<String> nextActiveNodes = new HashSet<>();

        // 找到起始节点并激活
        String startNodeId = findStartNodeId(flowchart);
        if (startNodeId == null) {
            return;
        }
        nextActiveNodes.add(startNodeId);

        // 外部变量快照（双缓冲：current 用于读，next 用于写）
        Map<String, Object> externalVarsCurrent = new HashMap<>(this.externalVarsCurrent);
        Map<String, Object> externalVarsNext = new HashMap<>(this.externalVarsNext);

        // 内部变量快照（双缓冲）
        Map<String, Object> internalVarsCurrent = new HashMap<>(this.internalVarsCurrent);
        Map<String, Object> internalVarsNext = new HashMap<>(this.internalVarsNext);

        // 节点映射
        Map<String, Node> nodeMap = mapNodes(flowchart);

        long tick = 0;

        // 主循环 - 真正的 Tick 驱动
        while (!activeNodes.isEmpty() || !nextActiveNodes.isEmpty()) {
            long tickStart = System.currentTimeMillis();
            tick++;

            // 1. 提交上一 Tick 的状态变更（双缓冲交换）
            Map<String, Object> temp = externalVarsCurrent;
            externalVarsCurrent = externalVarsNext;
            externalVarsNext = temp;

            temp = internalVarsCurrent;
            internalVarsCurrent = internalVarsNext;
            internalVarsNext = temp;
            internalVarsNext.clear();

            // 交换激活节点集合
            Set<String> tempSet = activeNodes;
            activeNodes = nextActiveNodes;
            nextActiveNodes = tempSet;
            nextActiveNodes.clear();

            // 2. 执行当前 Tick 中所有激活的节点
            for (String nodeId : activeNodes) {
                Node node = nodeMap.get(nodeId);
                if (node == null) {
                    continue;
                }

                // 更新节点状态为 ACTIVE
                nodeStates.put(nodeId, "ACTIVE");
                updateNodeStatus(nodeId, "ACTIVE", "Tick " + tick + " executing " + node.getType());

                // 执行节点逻辑
                executeNode(node, flowchart, nodeMap, externalVarsCurrent, internalVarsNext, nextActiveNodes, nodeStates);

                // 更新节点状态为 COMPLETED
                nodeStates.put(nodeId, "COMPLETED");
                updateNodeStatus(nodeId, "COMPLETED", "Tick " + tick + " finished " + node.getType());
            }

            // 3. 等待下一个 Tick
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
     * This replaces the old findNextNodeId that used findFirst() without considering semantics.
     */
    private String selectNextNodeId(Node currentNode, Flowchart flowchart, Map<String, Object> externalSnapshot) {
        String currentNodeId = currentNode.getId();
        List<Edge> outgoingEdges = flowchart.getEdges().stream()
                .filter(e -> e.getSource().equals(currentNodeId))
                .collect(Collectors.toList());

        // No outgoing edges - execution ends
        if (outgoingEdges.isEmpty()) {
            return null;
        }

        // Single outgoing edge - default behavior (backward compatibility)
        if (outgoingEdges.size() == 1) {
            return outgoingEdges.get(0).getTarget();
        }

        // Multiple outgoing edges - must use role-based or condition-based selection
        String nodeType = currentNode.getType();
        if (nodeType == null) {
            updateNodeStatus(currentNodeId, "ERROR", "Node type is null, cannot determine next node");
            return null;
        }

        String type = nodeType.toLowerCase(Locale.ROOT);

        if ("decision".equals(type)) {
            return selectDecisionNextNode(currentNode, outgoingEdges, externalSnapshot);
        } else if ("task".equals(type)) {
            return selectTaskNextNode(currentNode, outgoingEdges);
        } else if ("join".equals(type)) {
            return selectJoinNextNode(currentNode, outgoingEdges);
        } else {
            // For other node types, require explicit role on all edges
            return selectDefaultNextNode(currentNodeId, outgoingEdges);
        }
    }

    /**
     * Select next node for Decision node based on condition evaluation.
     */
    private String selectDecisionNextNode(Node decisionNode, List<Edge> outgoingEdges, Map<String, Object> externalSnapshot) {
        try {
            boolean conditionResult = evalDecisionCondition(decisionNode, externalSnapshot);
            String targetRole = conditionResult ? "true" : "false";

            // Find edge with matching role
            for (Edge edge : outgoingEdges) {
                Map<String, Object> data = edge.getData();
                if (data != null && targetRole.equals(data.get("role"))) {
                    return edge.getTarget();
                }
            }

            // No matching edge found - error
            updateNodeStatus(decisionNode.getId(), "ERROR",
                    "Decision result is " + conditionResult + " but no outgoing edge with role='" + targetRole + "' found");
            return null;
        } catch (Exception e) {
            updateNodeStatus(decisionNode.getId(), "ERROR",
                    "Failed to evaluate decision condition: " + e.getMessage());
            return null;
        }
    }

    /**
     * Select next node for Task node based on outcome role (onDone/onError/onTimeout).
     */
    private String selectTaskNextNode(Node taskNode, List<Edge> outgoingEdges) {
        // For now, default to onDone
        // In the future, this could be determined by actual task execution outcome
        String targetRole = "onDone";

        // Check if there's a simulated outcome in node data
        Map<String, Object> nodeData = taskNode.getData();
        if (nodeData != null && nodeData.containsKey("simulatedOutcome")) {
            String outcome = (String) nodeData.get("simulatedOutcome");
            if ("onError".equals(outcome) || "onTimeout".equals(outcome)) {
                targetRole = outcome;
            }
        }

        // Find edge with matching role
        for (Edge edge : outgoingEdges) {
            Map<String, Object> data = edge.getData();
            if (data != null && targetRole.equals(data.get("role"))) {
                return edge.getTarget();
            }
        }

        // No matching edge found - error
        updateNodeStatus(taskNode.getId(), "ERROR",
                "Task outcome is " + targetRole + " but no outgoing edge with role='" + targetRole + "' found");
        return null;
    }

    /**
     * Select next node for Join node based on onReady role.
     */
    private String selectJoinNextNode(Node joinNode, List<Edge> outgoingEdges) {
        String targetRole = "onReady";

        // Find edge with matching role
        for (Edge edge : outgoingEdges) {
            Map<String, Object> data = edge.getData();
            if (data != null && targetRole.equals(data.get("role"))) {
                return edge.getTarget();
            }
        }

        // No matching edge found - error
        updateNodeStatus(joinNode.getId(), "ERROR",
                "Join node requires an outgoing edge with role='" + targetRole + "'");
        return null;
    }

    /**
     * Select next node for default node types (start, end, action) based on role.
     */
    private String selectDefaultNextNode(String nodeId, List<Edge> outgoingEdges) {
        // Check if all edges have roles
        boolean allHaveRoles = true;
        for (Edge edge : outgoingEdges) {
            Map<String, Object> data = edge.getData();
            if (data == null || !data.containsKey("role")) {
                allHaveRoles = false;
                break;
            }
        }

        if (!allHaveRoles) {
            updateNodeStatus(nodeId, "ERROR",
                    "Multiple outgoing edges but not all have roles defined");
            return null;
        }

        // For now, select the first edge with a role
        // In the future, this could be enhanced with more sophisticated selection logic
        return outgoingEdges.get(0).getTarget();
    }

    /**
     * Evaluate decision condition based on external snapshot.
     * Currently supports:
     * - left.source = "external" with name
     * - op = EQ/NEQ
     * - right.literal (any value)
     */
    private boolean evalDecisionCondition(Node decisionNode, Map<String, Object> externalSnapshot) {
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

        if (!"external".equals(source)) {
            throw new RuntimeException("Only external variable source is supported, got: " + source);
        }

        if (name == null || name.isEmpty()) {
            throw new RuntimeException("External variable name is empty");
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

        // Get external variable value
        Object varValue = externalSnapshot.get(name);
        if (varValue == null) {
            throw new RuntimeException("External variable '" + name + "' not found in snapshot");
        }

        // Compare values
        boolean result;
        if ("EQ".equals(op)) {
            result = Objects.equals(varValue, literal);
        } else { // NEQ
            result = !Objects.equals(varValue, literal);
        }

        return result;
    }

    /**
     * 执行单个节点 - 简单直接，不搞过度抽象
     */
    private void executeNode(Node node, Flowchart flowchart, Map<String, Node> nodeMap, 
                           Map<String, Object> externalVarsCurrent, Map<String, Object> internalVarsNext,
                           Set<String> nextActiveNodes, Map<String, String> nodeStates) {
        String type = node.getType();
        if (type == null) {
            return;
        }

        String t = type.toLowerCase(Locale.ROOT);

        if ("action".equals(t)) {
            // Action 节点立即完成，激活下一个节点
            String nextNodeId = selectNextNodeId(node, flowchart, externalVarsCurrent);
            if (nextNodeId != null) {
                nextActiveNodes.add(nextNodeId);
            }
        } else if ("task".equals(t)) {
            // Task 节点：检查是否在等待状态
            if ("WAITING".equals(nodeStates.get(node.getId()))) {
                // 检查任务是否完成
                String taskState = getTaskState(node);

                if ("Done".equals(taskState)) {
                    nodeStates.put(node.getId(), "COMPLETED");
                    String nextNodeId = selectNextNodeIdByRole(node, flowchart, "onDone");
                    if (nextNodeId != null) {
                        nextActiveNodes.add(nextNodeId);
                    }
                } else if ("Error".equals(taskState)) {
                    nodeStates.put(node.getId(), "ERROR");
                    String nextNodeId = selectNextNodeIdByRole(node, flowchart, "onError");
                    if (nextNodeId != null) {
                        nextActiveNodes.add(nextNodeId);
                    }
                } else if ("Timeout".equals(taskState)) {
                    nodeStates.put(node.getId(), "ERROR");
                    String nextNodeId = selectNextNodeIdByRole(node, flowchart, "onTimeout");
                    if (nextNodeId != null) {
                        nextActiveNodes.add(nextNodeId);
                    }
                }
                // 仍在 Running，继续等待
            } else {
                // 首次激活 Task，启动能力并进入等待状态
                startCapability(node);
                nodeStates.put(node.getId(), "WAITING");
                nextActiveNodes.add(node.getId()); // 下一 tick 继续检查
            }
        } else if ("decision".equals(t)) {
            // Decision 节点：立即评估条件并选择分支
            try {
                boolean conditionResult = evalDecisionCondition(node, externalVarsCurrent);
                String targetRole = conditionResult ? "true" : "false";
                String nextNodeId = selectNextNodeIdByRole(node, flowchart, targetRole);
                if (nextNodeId != null) {
                    nextActiveNodes.add(nextNodeId);
                } else {
                    nodeStates.put(node.getId(), "ERROR");
                    updateNodeStatus(node.getId(), "ERROR", 
                            "Decision result is " + conditionResult + " but no outgoing edge with role='" + targetRole + "' found");
                }
            } catch (Exception e) {
                nodeStates.put(node.getId(), "ERROR");
                updateNodeStatus(node.getId(), "ERROR", 
                        "Failed to evaluate decision condition: " + e.getMessage());
            }
        } else if ("join".equals(t)) {
            // Join 节点：检查是否满足汇合条件
            if (checkJoinCondition(node, nodeStates)) {
                String nextNodeId = selectNextNodeIdByRole(node, flowchart, "onReady");
                if (nextNodeId != null) {
                    nextActiveNodes.add(nextNodeId);
                } else {
                    nodeStates.put(node.getId(), "ERROR");
                    updateNodeStatus(node.getId(), "ERROR", 
                            "Join node requires an outgoing edge with role='onReady'");
                }
            } else {
                // 不满足条件，继续等待
                nodeStates.put(node.getId(), "WAITING");
                nextActiveNodes.add(node.getId());
            }
        } else {
            // 其他节点类型：简单处理，激活下一个节点
            String nextNodeId = selectNextNodeId(node, flowchart, externalVarsCurrent);
            if (nextNodeId != null) {
                nextActiveNodes.add(nextNodeId);
            }
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
     * 获取任务状态 - 简单实现，后续可接入真实能力
     */
    private String getTaskState(Node taskNode) {
        // 简单实现：从节点数据中读取模拟状态
        Map<String, Object> nodeData = taskNode.getData();
        if (nodeData != null && nodeData.containsKey("simulatedState")) {
            return (String) nodeData.get("simulatedState");
        }
        // 默认返回 Done
        return "Done";
    }

    /**
     * 启动能力 - 简单实现，后续可接入真实能力
     */
    private void startCapability(Node taskNode) {
        // 简单实现：设置模拟状态为 Running
        Map<String, Object> nodeData = taskNode.getData();
        if (nodeData == null) {
            nodeData = new HashMap<>();
            taskNode.setData(nodeData);
        }
        nodeData.put("simulatedState", "Running");

        // 简单实现：模拟能力在 3 个 tick 后完成
        // 实际实现中，这里应该调用 Host 接口启动能力
        // Host 会在后续 tick 中更新能力状态
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
