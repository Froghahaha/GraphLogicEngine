package com.example.graphlogic.capability;

public record CapabilityMetadata(
        String id,
        String version,
        String description,
        long recommendedTimeoutMs
) {
}
