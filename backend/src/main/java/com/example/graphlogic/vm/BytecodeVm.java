package com.example.graphlogic.vm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;

public final class BytecodeVm {
    private final Program program;
    private final HostBinding host;
    private final int maxStepsPerTick;
    private final VmState state;

    public BytecodeVm(Program program, HostBinding host, int maxStepsPerTick, int entryPc) {
        if (program == null) throw new IllegalArgumentException("program is null");
        if (host == null) throw new IllegalArgumentException("host is null");
        if (maxStepsPerTick <= 0) throw new IllegalArgumentException("maxStepsPerTick must be > 0");
        if (entryPc < 0) throw new IllegalArgumentException("entryPc must be >= 0");
        this.program = program;
        this.host = host;
        this.maxStepsPerTick = maxStepsPerTick;
        this.state = new VmState(entryPc);
    }

    public boolean halted() {
        return state.frame.halted;
    }

    public String lastNodeId() {
        return state.lastNodeId;
    }

    public void runTick() {
        if (state.frame.halted) {
            return;
        }
        state.internalNext = new java.util.HashMap<>(state.internalCurrent);
        state.frame.waiting = false;

        int steps = 0;
        byte[] code = program.code();
        InstructionReader r = new InstructionReader(code, state.frame.pc);

        while (!state.frame.halted && !state.frame.waiting) {
            if (steps++ >= maxStepsPerTick) {
                break;
            }
            if (r.pc() < 0 || r.pc() >= code.length) {
                throw new IllegalStateException("PC out of range: " + r.pc());
            }

            int opPc = r.pc();
            String nodeId = program.debugPcToNodeId().get(r.pc());
            if (nodeId != null) {
                state.lastNodeId = nodeId;
            }

            int op = r.readOpcode();

            switch (op) {
                case Opcode.PUSH_CONST -> {
                    int id = r.readU16();
                    Object val = program.constants().get(id);
                    state.stack.push(val);
                }
                case Opcode.LOAD_EXT -> {
                    int id = r.readU16();
                    String name = program.externalSymbolsById().get(id);
                    state.stack.push(host.readExternal(name));
                }
                case Opcode.LOAD_INT -> {
                    int id = r.readU16();
                    state.stack.push(state.internalCurrent.getOrDefault(id, 0));
                }
                case Opcode.STORE_INT -> {
                    int id = r.readU16();
                    Object val = state.stack.pop();
                    state.internalNext.put(id, val);
                }
                case Opcode.CMP_EQ -> {
                    Object b = state.stack.pop();
                    Object a = state.stack.pop();
                    state.stack.push(Objects.equals(a, b));
                }
                case Opcode.NOT -> {
                    Object a = state.stack.pop();
                    if (!(a instanceof Boolean)) {
                        throw new IllegalStateException("NOT expects boolean, got: " + a);
                    }
                    state.stack.push(!((Boolean) a));
                }
                case Opcode.JMP -> {
                    int off = r.readS16();
                    r.setPc(r.pc() + off);
                }
                case Opcode.JMP_T -> {
                    int off = r.readS16();
                    Object a = state.stack.pop();
                    if (!(a instanceof Boolean)) {
                        throw new IllegalStateException("JMP_T expects boolean, got: " + a);
                    }
                    if ((Boolean) a) {
                        r.setPc(r.pc() + off);
                    }
                }
                case Opcode.JMP_F -> {
                    int off = r.readS16();
                    Object a = state.stack.pop();
                    if (!(a instanceof Boolean)) {
                        throw new IllegalStateException("JMP_F expects boolean, got: " + a);
                    }
                    if (!((Boolean) a)) {
                        r.setPc(r.pc() + off);
                    }
                }
                case Opcode.CALL_ACTION -> {
                    int id = r.readU16();
                    int argc = r.readU8();
                    if (argc != 1) {
                        throw new IllegalStateException("CALL_ACTION expects argc=1 (payload map), got: " + argc);
                    }
                    Object payload = state.stack.pop();
                    if (!(payload instanceof Map)) {
                        throw new IllegalStateException("CALL_ACTION payload must be Map, got: " + payload);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) payload;
                    String actionId = program.actionIdsById().get(id);
                    int handle = host.emitAction(actionId, params);
                    state.stack.push(handle);
                }
                case Opcode.WAIT_ACTION -> {
                    Object a = state.stack.pop();
                    if (!(a instanceof Integer)) {
                        throw new IllegalStateException("WAIT_ACTION expects ActionHandle(int), got: " + a);
                    }
                    HostBinding.ActionPoll poll = host.pollAction((Integer) a);
                    if (!poll.finished()) {
                        state.stack.push(a);
                        r.setPc(opPc);
                        state.frame.waiting = true;
                    } else {
                        state.stack.push(poll.category());
                    }
                }
                case Opcode.END -> state.frame.halted = true;
                case Opcode.YIELD -> {
                    break;
                }
                default -> throw new IllegalStateException("Unknown opcode: 0x" + Integer.toHexString(op));
            }
        }

        state.frame.pc = r.pc();
        state.internalCurrent.clear();
        state.internalCurrent.putAll(state.internalNext);
    }
}
