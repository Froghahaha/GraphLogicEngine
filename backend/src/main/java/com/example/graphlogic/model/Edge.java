package com.example.graphlogic.model;

import lombok.Data;
import java.util.Map;

@Data
public class Edge {
    private String id;
    private String source;
    private String target;
    private String type; // default
    private Map<String, Object> data; // Custom data, including role field for branch selection
}
