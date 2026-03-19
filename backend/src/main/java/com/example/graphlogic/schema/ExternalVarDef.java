package com.example.graphlogic.schema;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExternalVarDef {

    private String name;
    private String type;
    private String category;
    private String description;
}

