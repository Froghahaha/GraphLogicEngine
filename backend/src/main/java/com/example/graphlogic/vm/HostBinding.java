package com.example.graphlogic.vm;

import java.util.Map;

public interface HostBinding {
    long nowMillis();

    Object readExternal(String symbolName);

    int emitAction(String actionId, Map<String, Object> params);

    ActionPoll pollAction(int handle);

    record ActionPoll(boolean finished, int category) {
    }
}
