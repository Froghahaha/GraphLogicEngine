package com.example.graphlogic.schema;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schema")
@CrossOrigin(origins = "*")
public class VariableSchemaController {

    @GetMapping("/external-vars")
    public List<ExternalVarDef> externalVars() {
        return List.of(
                new ExternalVarDef("sensor_start", "bool", "io", "Start sensor"),
                new ExternalVarDef("sensor_stop", "bool", "io", "Stop sensor"),
                new ExternalVarDef("WorkpieceExist", "bool", "robot", "Workpiece existence flag")
        );
    }

    @GetMapping("/internal-vars")
    public List<InternalVarDef> internalVars() {
        return List.of(
                new InternalVarDef("_stm_state", "int", false, "Main state machine state")
        );
    }
}
