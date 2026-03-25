package com.example.graphlogic.compiler;

import com.example.graphlogic.model.Edge;
import com.example.graphlogic.model.Flowchart;
import com.example.graphlogic.model.Node;
import com.example.graphlogic.vm.Opcode;
import com.example.graphlogic.vm.Program;

import java.util.*;

public final class GraphCompiler {
    private static final int TMP_CATEGORY_SYMBOL_ID = 1;

    public Program compile(Flowchart flowchart) {
        if (flowchart == null) throw new CompileException("flowchart is null");
        if (flowchart.getNodes() == null) throw new CompileException("flowchart.nodes is null");
        if (flowchart.getEdges() == null) throw new CompileException("flowchart.edges is null");

        Map<String, Node> nodesById = new HashMap<>();
        for (Node n : flowchart.getNodes()) {
            if (n.getId() == null || n.getId().isEmpty()) throw new CompileException("node.id is empty");
            if (nodesById.put(n.getId(), n) != null) throw new CompileException("duplicate node id: " + n.getId());
        }

        String startId = findSingleStartNodeId(flowchart.getNodes());
        ProgramBuilder b = new ProgramBuilder();

        b.label("entry");
        b.jmpLabel(labelOf(startId));

        List<String> ordered = reachableNodesInDeterministicOrder(flowchart, startId);
        for (String nodeId : ordered) {
            Node node = nodesById.get(nodeId);
            if (node == null) throw new CompileException("missing node: " + nodeId);

            b.label(labelOf(nodeId));
            b.markNodePc(nodeId);
            emitNode(b, flowchart, node);
        }

        return b.build();
    }

    private void emitNode(ProgramBuilder b, Flowchart flowchart, Node node) {
        String type = normType(node.getType());
        if (type == null) throw new CompileException("node.type is null for node " + node.getId());

        switch (type) {
            case "start" -> emitStart(b, flowchart, node);
            case "end" -> {
                b.op(Opcode.END);
            }
            case "decision" -> emitDecision(b, flowchart, node);
            case "task" -> emitTask(b, flowchart, node);
            default -> throw new CompileException("unsupported node type: " + node.getType() + " (node " + node.getId() + ")");
        }
    }

    private void emitStart(ProgramBuilder b, Flowchart flowchart, Node node) {
        String next = soleOutgoingTarget(flowchart, node.getId());
        b.jmpLabel(labelOf(next));
    }

    private void emitTask(ProgramBuilder b, Flowchart flowchart, Node node) {
        Map<String, Object> data = node.getData();
        if (data == null) throw new CompileException("task node has no data: " + node.getId());
        Object capIdObj = data.get("capabilityId");
        if (!(capIdObj instanceof String) || ((String) capIdObj).isEmpty()) {
            throw new CompileException("task node missing capabilityId: " + node.getId());
        }
        String capId = (String) capIdObj;

        Object paramsObj = data.get("capabilityParams");
        if (paramsObj == null) {
            paramsObj = Map.of();
        }
        if (!(paramsObj instanceof Map)) {
            throw new CompileException("task capabilityParams must be object(Map): " + node.getId());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) paramsObj;

        int paramsConstId = b.addConst(params);
        int actionId = b.getOrAddActionId(capId);

        b.op(Opcode.PUSH_CONST);
        b.u16(paramsConstId);
        b.op(Opcode.CALL_ACTION);
        b.u16(actionId);
        b.u8(1);
        b.op(Opcode.WAIT_ACTION);

        b.op(Opcode.STORE_INT);
        b.u16(TMP_CATEGORY_SYMBOL_ID);

        Map<String, String> targets = roleTargets(flowchart, node.getId());
        requireRoleOnce(targets, "onSuccess", node.getId());
        requireRoleOnce(targets, "onRetry", node.getId());
        requireRoleOnce(targets, "onAbort", node.getId());
        requireRoleOnce(targets, "onTimeout", node.getId());

        emitCategoryBranch(b, 0, targets.get("onSuccess"));
        emitCategoryBranch(b, 1, targets.get("onRetry"));
        emitCategoryBranch(b, 2, targets.get("onAbort"));
        emitCategoryBranch(b, 3, targets.get("onTimeout"));

        b.op(Opcode.END);
    }

    private void emitCategoryBranch(ProgramBuilder b, int category, String targetNodeId) {
        int catConstId = b.addConst(category);

        b.op(Opcode.LOAD_INT);
        b.u16(TMP_CATEGORY_SYMBOL_ID);
        b.op(Opcode.PUSH_CONST);
        b.u16(catConstId);
        b.op(Opcode.CMP_EQ);
        b.jmpTLabel(labelOf(targetNodeId));
    }

    private void emitDecision(ProgramBuilder b, Flowchart flowchart, Node node) {
        Map<String, Object> data = node.getData();
        if (data == null) throw new CompileException("decision node has no data: " + node.getId());

        Object decisionObj = data.get("decision");
        if (!(decisionObj instanceof Map)) throw new CompileException("decision node missing decision data: " + node.getId());

        @SuppressWarnings("unchecked")
        Map<String, Object> decision = (Map<String, Object>) decisionObj;
        Object condObj = decision.get("condition");
        if (!(condObj instanceof Map)) throw new CompileException("decision node missing condition: " + node.getId());

        @SuppressWarnings("unchecked")
        Map<String, Object> cond = (Map<String, Object>) condObj;

        @SuppressWarnings("unchecked")
        Map<String, Object> left = (Map<String, Object>) cond.get("left");
        if (left == null) throw new CompileException("decision condition missing left: " + node.getId());

        String source = (String) left.get("source");
        String name = (String) left.get("name");
        if (!"external".equals(source)) {
            throw new CompileException("decision only supports left.source=external: " + node.getId());
        }
        if (name == null || name.isEmpty()) {
            throw new CompileException("decision left.name is empty: " + node.getId());
        }

        String op = (String) cond.get("op");
        if (!"EQ".equals(op) && !"NEQ".equals(op)) {
            throw new CompileException("decision op must be EQ or NEQ: " + node.getId());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> right = (Map<String, Object>) cond.get("right");
        if (right == null || !right.containsKey("literal")) {
            throw new CompileException("decision right.literal is required: " + node.getId());
        }
        Object literal = right.get("literal");
        int litConstId = b.addConst(literal);
        int symId = b.getOrAddExternalSymbol(name);

        b.op(Opcode.LOAD_EXT);
        b.u16(symId);
        b.op(Opcode.PUSH_CONST);
        b.u16(litConstId);
        b.op(Opcode.CMP_EQ);
        if ("NEQ".equals(op)) {
            b.op(Opcode.NOT);
        }

        Map<String, String> targets = roleTargets(flowchart, node.getId());
        requireRoleOnce(targets, "true", node.getId());
        requireRoleOnce(targets, "false", node.getId());

        b.jmpTLabel(labelOf(targets.get("true")));
        b.jmpLabel(labelOf(targets.get("false")));
    }

    private static String normType(String t) {
        if (t == null) return null;
        return t.trim().toLowerCase(Locale.ROOT);
    }

    private static String labelOf(String nodeId) {
        return "node:" + nodeId;
    }

    private static void requireRoleOnce(Map<String, String> targets, String role, String nodeId) {
        if (!targets.containsKey(role)) {
            throw new CompileException("missing outgoing edge role='" + role + "' for node " + nodeId);
        }
    }

    private static String findSingleStartNodeId(List<Node> nodes) {
        String start = null;
        for (Node n : nodes) {
            if ("start".equals(normType(n.getType()))) {
                if (start != null) throw new CompileException("multiple start nodes: " + start + ", " + n.getId());
                start = n.getId();
            }
        }
        if (start == null) throw new CompileException("no start node");
        return start;
    }

    private static String soleOutgoingTarget(Flowchart flowchart, String nodeId) {
        List<Edge> out = outgoingEdges(flowchart, nodeId);
        if (out.isEmpty()) throw new CompileException("node has no outgoing edge: " + nodeId);
        if (out.size() != 1) throw new CompileException("node must have exactly one outgoing edge: " + nodeId);
        return out.get(0).getTarget();
    }

    private static Map<String, String> roleTargets(Flowchart flowchart, String nodeId) {
        List<Edge> out = outgoingEdges(flowchart, nodeId);
        Map<String, String> m = new HashMap<>();
        for (Edge e : out) {
            Map<String, Object> data = e.getData();
            if (data == null || data.get("role") == null) {
                throw new CompileException("edge missing role: " + e.getId());
            }
            String role = String.valueOf(data.get("role"));
            String prev = m.put(role, e.getTarget());
            if (prev != null) {
                throw new CompileException("duplicate edge role '" + role + "' from node " + nodeId);
            }
        }
        return m;
    }

    private static List<Edge> outgoingEdges(Flowchart flowchart, String nodeId) {
        List<Edge> out = new ArrayList<>();
        for (Edge e : flowchart.getEdges()) {
            if (nodeId.equals(e.getSource())) {
                out.add(e);
            }
        }
        out.sort(Comparator.comparing((Edge e) -> String.valueOf(e.getData() != null ? e.getData().get("role") : ""))
                .thenComparing(Edge::getTarget));
        return out;
    }

    private static List<String> reachableNodesInDeterministicOrder(Flowchart flowchart, String startId) {
        Map<String, List<String>> adj = new HashMap<>();
        for (Edge e : flowchart.getEdges()) {
            adj.computeIfAbsent(e.getSource(), k -> new ArrayList<>()).add(e.getTarget());
        }
        for (List<String> lst : adj.values()) {
            lst.sort(String::compareTo);
        }

        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayDeque<String> q = new ArrayDeque<>();
        q.add(startId);
        while (!q.isEmpty()) {
            String id = q.removeFirst();
            if (!seen.add(id)) continue;
            for (String t : adj.getOrDefault(id, List.of())) {
                q.addLast(t);
            }
        }
        return new ArrayList<>(seen);
    }
}

