package com.example.graphlogic.capability;

import java.util.Map;

public class TimedMockCapability implements CapabilityInstance {

    private final String id;
    private final String version;
    private final long simulatedDurationMs;
    private final long recommendedTimeoutMs;
    private final String doneCode;
    private final CapabilityResultCategory doneCategory;

    private CapabilityLifecycleState lifecycleState = CapabilityLifecycleState.IDLE;
    private CapabilityResult result;
    private long startTimeMillis;

    public TimedMockCapability(String id,
                               String version,
                               long simulatedDurationMs,
                               long recommendedTimeoutMs,
                               String doneCode,
                               CapabilityResultCategory doneCategory) {
        this.id = id;
        this.version = version;
        this.simulatedDurationMs = simulatedDurationMs;
        this.recommendedTimeoutMs = recommendedTimeoutMs;
        this.doneCode = doneCode;
        this.doneCategory = doneCategory;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public CapabilityLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    @Override
    public CapabilityResult getResult() {
        return result;
    }

    @Override
    public void start(Map<String, Object> params, long nowMillis) {
        if (lifecycleState == CapabilityLifecycleState.RUNNING) {
            return;
        }
        lifecycleState = CapabilityLifecycleState.RUNNING;
        result = null;
        startTimeMillis = nowMillis;
    }

    @Override
    public void tick(long nowMillis) {
        if (lifecycleState != CapabilityLifecycleState.RUNNING) {
            return;
        }
        long elapsed = nowMillis - startTimeMillis;
        if (elapsed >= recommendedTimeoutMs) {
            lifecycleState = CapabilityLifecycleState.TIMEOUT;
            result = new CapabilityResult("TIMEOUT", CapabilityResultCategory.FATAL_ERROR, null);
            return;
        }
        if (elapsed >= simulatedDurationMs) {
            lifecycleState = CapabilityLifecycleState.DONE;
            result = new CapabilityResult(doneCode, doneCategory, null);
        }
    }
}

