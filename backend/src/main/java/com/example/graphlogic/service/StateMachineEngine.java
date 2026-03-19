package com.example.graphlogic.service;

import com.example.graphlogic.model.Flowchart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StateMachineEngine {

    @Autowired
    private TickExecutionEngine tickExecutionEngine;

    public void executeFlow(Flowchart flowchart) {
        tickExecutionEngine.execute(flowchart);
    }
}
