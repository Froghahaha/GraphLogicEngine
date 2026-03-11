package com.example.graphlogic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeStatus {
    private String nodeId;
    private String status; // ACTIVE, COMPLETED, ERROR
    private String message;
    private long timestamp;
}
