package com.example.graphlogic.vm;

public final class Opcode {
    private Opcode() {
    }

    public static final int PUSH_CONST = 0x01;
    public static final int LOAD_EXT = 0x02;
    public static final int LOAD_INT = 0x03;
    public static final int STORE_INT = 0x04;

    public static final int CMP_EQ = 0x20;
    public static final int NOT = 0x31;

    public static final int JMP = 0x40;
    public static final int JMP_T = 0x41;
    public static final int JMP_F = 0x42;
    public static final int END = 0x43;
    public static final int YIELD = 0x44;

    public static final int CALL_ACTION = 0x50;
    public static final int WAIT_ACTION = 0x51;
}
