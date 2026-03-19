package com.example.graphlogic.model;

import com.example.graphlogic.schema.LogicalInternalVarDef;
import lombok.Data;
import java.util.List;

@Data
public class Flowchart {
    private String id;
    private String name;
    private List<Node> nodes;
    private List<Edge> edges;
    private List<LogicalInternalVarDef> internalVars;
}
