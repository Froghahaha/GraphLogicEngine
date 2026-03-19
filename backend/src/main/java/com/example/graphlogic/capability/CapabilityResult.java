package com.example.graphlogic.capability;

public final class CapabilityResult {

    private final String code;
    private final CapabilityResultCategory category;
    private final Object data;

    public CapabilityResult(String code, CapabilityResultCategory category, Object data) {
        this.code = code;
        this.category = category;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public CapabilityResultCategory getCategory() {
        return category;
    }

    public Object getData() {
        return data;
    }
}

