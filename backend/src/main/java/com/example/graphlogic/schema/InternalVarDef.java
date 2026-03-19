package com.example.graphlogic.schema;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InternalVarDef {

    private String name;
    private String type;
    private boolean persistent;
    private String description;
}

