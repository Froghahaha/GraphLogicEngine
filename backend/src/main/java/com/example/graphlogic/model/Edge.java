package com.example.graphlogic.model;

import lombok.Data;

@Data
public class Edge {
    private String id;
    private String source;
    private String target;
    private String type; // default
}
