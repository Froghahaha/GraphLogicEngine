package com.example.graphlogic.model;

import lombok.Data;
import java.util.List;

@Data
public class Flowchart {
    private String id;
    private String name;
    private List<Node> nodes;
    private List<Edge> edges;
}
