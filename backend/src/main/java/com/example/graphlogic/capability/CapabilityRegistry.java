package com.example.graphlogic.capability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CapabilityRegistry {

    private final Map<String, CapabilityInstance> instances = new ConcurrentHashMap<>();

    public void register(CapabilityInstance instance) {
        if (instance == null) {
            return;
        }
        instances.put(instance.id(), instance);
    }

    public CapabilityInstance get(String id) {
        if (id == null) {
            return null;
        }
        return instances.get(id);
    }
}

