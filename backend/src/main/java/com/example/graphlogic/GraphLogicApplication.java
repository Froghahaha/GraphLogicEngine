package com.example.graphlogic;

import com.example.graphlogic.capability.*;
import com.example.graphlogic.service.TickExecutionEngine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

@SpringBootApplication
public class GraphLogicApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphLogicApplication.class, args);
    }

    @Bean
    public CapabilityRegistry capabilityRegistry() {
        CapabilityRegistry registry = new CapabilityRegistry();

        registry.register(new TimedMockCapability("MoveToPose", "1.0.0", 500, 5000, "OK", CapabilityResultCategory.SUCCESS,
                new CapabilityMetadata("MoveToPose", "1.0.0", "Move robot arm", 5000)));

        registry.register(new TimedMockCapability("PickObject", "1.0.0", 800, 5000, "NO_OBJECT", CapabilityResultCategory.RECOVERABLE_ERROR,
                new CapabilityMetadata("PickObject", "1.0.0", "Pick object", 5000)));

        registry.register(new TimedMockCapability("ScanQRCode", "1.0.0", 1000, 5000, "VISION_LOST", CapabilityResultCategory.FATAL_ERROR,
                new CapabilityMetadata("ScanQRCode", "1.0.0", "Scan QR code", 5000)));

        return registry;
    }

    @Bean
    public TickExecutionEngine tickExecutionEngine(SimpMessagingTemplate messagingTemplate, 
                                               CapabilityRegistry capabilityRegistry) {
        return new TickExecutionEngine(messagingTemplate, 10, capabilityRegistry);
    }
}
