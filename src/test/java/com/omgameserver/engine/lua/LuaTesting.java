package com.omgameserver.engine.lua;

import com.crionuke.bolts.Dispatcher;
import org.luaj.vm2.LuaTable;

public class LuaTesting extends LuaTable {

    private final String FUNCTION_TICK_EVENT_RECEIVED = "tick_event_received";

    public LuaTesting(Dispatcher dispatcher) {
        set(FUNCTION_TICK_EVENT_RECEIVED, new LuaTickEventReceivedFunction(dispatcher));
    }
}
