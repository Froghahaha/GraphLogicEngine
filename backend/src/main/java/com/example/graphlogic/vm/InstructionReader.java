package com.example.graphlogic.vm;

public final class InstructionReader {
    private final byte[] code;
    private int pc;

    public InstructionReader(byte[] code, int pc) {
        if (code == null) throw new IllegalArgumentException("code is null");
        if (pc < 0) throw new IllegalArgumentException("pc must be >= 0");
        this.code = code;
        this.pc = pc;
    }

    public int pc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int readOpcode() {
        return readU8();
    }

    public int readU8() {
        if (pc >= code.length) {
            throw new IllegalStateException("Unexpected EOF");
        }
        return code[pc++] & 0xFF;
    }

    public int readU16() {
        int lo = readU8();
        int hi = readU8();
        return (hi << 8) | lo;
    }

    public int readS16() {
        return (short) readU16();
    }
}

