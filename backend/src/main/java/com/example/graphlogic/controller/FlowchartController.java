package com.example.graphlogic.controller;

import com.example.graphlogic.model.Flowchart;
import com.example.graphlogic.service.StateMachineEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flow")
@CrossOrigin(origins = "*") // Allow frontend
public class FlowchartController {

    @Autowired
    private StateMachineEngine engine;

    @PostMapping("/execute")
    public ResponseEntity<String> executeFlow(@RequestBody Flowchart flowchart) {
        System.out.println("Received flowchart: " + flowchart.getId());
        engine.executeFlow(flowchart);
        return ResponseEntity.ok("Execution started for " + flowchart.getId());
    }
}
