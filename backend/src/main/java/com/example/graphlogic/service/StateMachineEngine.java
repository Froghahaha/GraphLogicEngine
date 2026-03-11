package com.example.graphlogic.service;

import com.example.graphlogic.model.Flowchart;
import com.example.graphlogic.model.Node;
import com.example.graphlogic.model.NodeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StateMachineEngine {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void executeFlow(Flowchart flowchart) {
        // Map nodes by ID for easy lookup
        Map<String, Node> nodeMap = flowchart.getNodes().stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        // Find Start Node
        Node startNode = flowchart.getNodes().stream()
                .filter(n -> "start".equalsIgnoreCase(n.getType()) || "input".equalsIgnoreCase(n.getType()))
                .findFirst()
                .orElse(null);

        if (startNode != null) {
            executeNode(startNode, nodeMap, flowchart);
        } else {
            System.err.println("No start node found!");
        }
    }

    private void executeNode(Node node, Map<String, Node> nodeMap, Flowchart flowchart) {
        // 1. Send ACTIVE status
        sendStatus(node.getId(), "ACTIVE", "Executing " + node.getType());

        // 2. Async Execution (Simulation)
        CompletableFuture.runAsync(() -> {
            try {
                // Simulate work
                if ("action".equalsIgnoreCase(node.getType())) {
                    Thread.sleep(2000); // 2 seconds for action
                } else {
                    Thread.sleep(500); // 0.5s for others
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).thenRun(() -> {
            // 3. Send COMPLETED status
            sendStatus(node.getId(), "COMPLETED", "Finished " + node.getType());

            // 4. Find next node
            String nextNodeId = findNextNodeId(node.getId(), flowchart);
            if (nextNodeId != null && nodeMap.containsKey(nextNodeId)) {
                executeNode(nodeMap.get(nextNodeId), nodeMap, flowchart);
            }
        });
    }

    private String findNextNodeId(String currentId, Flowchart flowchart) {
        return flowchart.getEdges().stream()
                .filter(e -> e.getSource().equals(currentId))
                .map(e -> e.getTarget())
                .findFirst()
                .orElse(null);
    }

    private void sendStatus(String nodeId, String status, String message) {
        NodeStatus nodeStatus = new NodeStatus(nodeId, status, message, System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/status", nodeStatus);
    }
}
