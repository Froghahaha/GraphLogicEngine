package com.example.graphlogic.vm;

import java.util.List;
import java.util.Map;

public final class Program {
    private final byte[] code;
    private final List<Object> constants;
    private final List<String> externalSymbolsById;
    private final List<String> actionIdsById;
    private final Map<Integer, String> debugPcToNodeId;

    public Program(
            byte[] code,
            List<Object> constants,
            List<String> externalSymbolsById,
            List<String> actionIdsById,
            Map<Integer, String> debugPcToNodeId
    ) {
        if (code == null) throw new IllegalArgumentException("code is null");
        if (constants == null) throw new IllegalArgumentException("constants is null");
        if (externalSymbolsById == null) throw new IllegalArgumentException("externalSymbolsById is null");
        if (actionIdsById == null) throw new IllegalArgumentException("actionIdsById is null");
        if (debugPcToNodeId == null) throw new IllegalArgumentException("debugPcToNodeId is null");
        this.code = code;
        this.constants = constants;
        this.externalSymbolsById = externalSymbolsById;
        this.actionIdsById = actionIdsById;
        this.debugPcToNodeId = debugPcToNodeId;
    }

    public byte[] code() {
        return code;
    }

    public List<Object> constants() {
        return constants;
    }

    public List<String> externalSymbolsById() {
        return externalSymbolsById;
    }

    public List<String> actionIdsById() {
        return actionIdsById;
    }

    public Map<Integer, String> debugPcToNodeId() {
        return debugPcToNodeId;
    }
}
