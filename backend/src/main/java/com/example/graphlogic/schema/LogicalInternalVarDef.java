package com.example.graphlogic.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogicalInternalVarDef {

    private String name;
    private String type;
    private String initial;
    private String description;
}

