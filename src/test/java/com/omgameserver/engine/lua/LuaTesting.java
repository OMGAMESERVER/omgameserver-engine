package com.omgameserver.engine.lua;

import com.omgameserver.engine.OmgsDispatcher;
import org.luaj.vm2.LuaTable;

public class LuaTesting extends LuaTable {

    private final String FUNCTION_TICK_EVENT_RECEIVED = "tick_event_received";

    public LuaTesting(OmgsDispatcher dispatcher) {
        set(FUNCTION_TICK_EVENT_RECEIVED, new LuaTickEventReceivedFunction(dispatcher));
    }
}
