package com.example.graphlogic.schema;

import com.example.graphlogic.capability.CapabilityMetadata;
import com.example.graphlogic.capability.CapabilityRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schema")
@CrossOrigin(origins = "*")
public class VariableSchemaController {

    private final CapabilityRegistry capabilityRegistry;

    @Autowired
    public VariableSchemaController(CapabilityRegistry capabilityRegistry) {
        this.capabilityRegistry = capabilityRegistry;
    }

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

    @GetMapping("/capabilities")
    public List<CapabilityMetadata> capabilities() {
        return capabilityRegistry.getAll().stream()
                .map(instance -> instance.getMetadata())
                .collect(Collectors.toList());
    }
}
