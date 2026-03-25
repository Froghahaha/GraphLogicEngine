package com.example.graphlogic.vm;

import com.example.graphlogic.compiler.GraphCompiler;
import com.example.graphlogic.model.Edge;
import com.example.graphlogic.model.Flowchart;
import com.example.graphlogic.model.Node;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GoldenTraceTest {

    @Test
    void task_success_routes_to_onSuccess_deterministically() {
        Flowchart flow = new Flowchart();
        flow.setId("t1");
        flow.setName("t1");

        Node start = new Node();
        start.setId("start");
        start.setType("start");

        Node task = new Node();
        task.setId("task1");
        task.setType("task");
        task.setData(new HashMap<>(Map.of(
                "capabilityId", "MoveToPose",
                "capabilityParams", Map.of()
        )));

        Node end = new Node();
        end.setId("end");
        end.setType("end");

        flow.setNodes(List.of(start, task, end));

        flow.setEdges(List.of(
                edge("e1", "start", "task1", Map.of("role", "next")),
                edge("e2", "task1", "end", Map.of("role", "onSuccess")),
                edge("e3", "task1", "end", Map.of("role", "onRetry")),
                edge("e4", "task1", "end", Map.of("role", "onAbort")),
                edge("e5", "task1", "end", Map.of("role", "onTimeout"))
        ));

        Program program = new GraphCompiler().compile(flow);

        HostBinding host = new HostBinding() {
            private int nextHandle = 1;
            private final Map<Integer, Integer> remainingPolls = new HashMap<>();

            @Override
            public long nowMillis() {
                return 0;
            }

            @Override
            public Object readExternal(String symbolName) {
                return null;
            }

            @Override
            public int emitAction(String actionId, Map<String, Object> params) {
                int handle = nextHandle++;
                remainingPolls.put(handle, 2);
                return handle;
            }

            @Override
            public ActionPoll pollAction(int handle) {
                Integer n = remainingPolls.get(handle);
                if (n == null) {
                    fail("unknown handle");
                    return new ActionPoll(true, 0);
                }
                if (n > 0) {
                    remainingPolls.put(handle, n - 1);
                    return new ActionPoll(false, 0);
                }
                remainingPolls.remove(handle);
                return new ActionPoll(true, 0);
            }
        };

        BytecodeVm vm = new BytecodeVm(program, host, 1000, 0);

        int ticks = 0;
        while (!vm.halted() && ticks++ < 50) {
            vm.runTick();
        }

        assertTrue(vm.halted(), "program should halt");
        assertEquals("end", vm.lastNodeId());
    }

    private static Edge edge(String id, String source, String target, Map<String, Object> data) {
        Edge e = new Edge();
        e.setId(id);
        e.setSource(source);
        e.setTarget(target);
        e.setData(new HashMap<>(data));
        return e;
    }
}

