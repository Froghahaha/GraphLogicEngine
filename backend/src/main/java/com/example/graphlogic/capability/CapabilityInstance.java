package com.example.graphlogic.capability;

import java.util.Map;

public interface CapabilityInstance {

    String id();

    String version();

    CapabilityLifecycleState getLifecycleState();

    CapabilityResult getResult();

    void start(Map<String, Object> params, long nowMillis);

    void tick(long nowMillis);
}

