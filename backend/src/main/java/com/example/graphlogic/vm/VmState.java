package com.example.graphlogic.vm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class VmState {
    public final Frame frame;
    public final Deque<Object> stack = new ArrayDeque<>();
    public final Map<Integer, Object> internalCurrent = new HashMap<>();
    public Map<Integer, Object> internalNext = new HashMap<>();

    public String lastNodeId;

    public VmState(int entryPc) {
        this.frame = new Frame(entryPc);
    }
}

