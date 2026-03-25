package com.example.graphlogic.vm;

public final class Frame {
    int pc;
    boolean halted;
    boolean waiting;

    public Frame(int entryPc) {
        this.pc = entryPc;
    }
}
