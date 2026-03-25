package com.example.graphlogic.compiler;

import com.example.graphlogic.vm.Opcode;
import com.example.graphlogic.vm.Program;

import java.io.ByteArrayOutputStream;
import java.util.*;

public final class ProgramBuilder {
    private final ByteArrayOutputStream code = new ByteArrayOutputStream();
    private final List<Object> constants = new ArrayList<>();
    private final List<String> externalSymbolsById = new ArrayList<>();
    private final List<String> actionIdsById = new ArrayList<>();
    private final Map<Integer, String> debugPcToNodeId = new HashMap<>();

    private final Map<String, Integer> labelToPc = new HashMap<>();
    private final List<Fixup> fixups = new ArrayList<>();

    private record Fixup(int atPc, String label) {
    }

    public int pc() {
        return code.size();
    }

    public int addConst(Object value) {
        int id = constants.size();
        constants.add(value);
        return id;
    }

    public int getOrAddExternalSymbol(String name) {
        int idx = externalSymbolsById.indexOf(name);
        if (idx >= 0) return idx;
        externalSymbolsById.add(name);
        return externalSymbolsById.size() - 1;
    }

    public int getOrAddActionId(String id) {
        int idx = actionIdsById.indexOf(id);
        if (idx >= 0) return idx;
        actionIdsById.add(id);
        return actionIdsById.size() - 1;
    }

    public void markNodePc(String nodeId) {
        debugPcToNodeId.put(pc(), nodeId);
    }

    public void label(String name) {
        Integer prev = labelToPc.put(name, pc());
        if (prev != null) {
            throw new CompileException("Duplicate label: " + name);
        }
    }

    public void op(int opcode) {
        code.write(opcode);
    }

    public void u8(int v) {
        if (v < 0 || v > 255) throw new CompileException("u8 out of range: " + v);
        code.write(v & 0xFF);
    }

    public void u16(int v) {
        if (v < 0 || v > 0xFFFF) throw new CompileException("u16 out of range: " + v);
        code.write(v & 0xFF);
        code.write((v >> 8) & 0xFF);
    }

    public void s16(int v) {
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE) throw new CompileException("s16 out of range: " + v);
        u16(v & 0xFFFF);
    }

    public void jmpLabel(String label) {
        op(Opcode.JMP);
        int at = pc();
        s16(0);
        fixups.add(new Fixup(at, label));
    }

    public void jmpTLabel(String label) {
        op(Opcode.JMP_T);
        int at = pc();
        s16(0);
        fixups.add(new Fixup(at, label));
    }

    public void jmpFLabel(String label) {
        op(Opcode.JMP_F);
        int at = pc();
        s16(0);
        fixups.add(new Fixup(at, label));
    }

    public Program build() {
        byte[] bytes = code.toByteArray();
        for (Fixup f : fixups) {
            Integer targetPc = labelToPc.get(f.label());
            if (targetPc == null) {
                throw new CompileException("Unknown label: " + f.label());
            }
            int rel = targetPc - (f.atPc() + 2);
            if (rel < Short.MIN_VALUE || rel > Short.MAX_VALUE) {
                throw new CompileException("Jump too far: " + f.label());
            }
            bytes[f.atPc()] = (byte) (rel & 0xFF);
            bytes[f.atPc() + 1] = (byte) ((rel >> 8) & 0xFF);
        }

        return new Program(bytes, List.copyOf(constants), List.copyOf(externalSymbolsById), List.copyOf(actionIdsById), Map.copyOf(debugPcToNodeId));
    }
}

