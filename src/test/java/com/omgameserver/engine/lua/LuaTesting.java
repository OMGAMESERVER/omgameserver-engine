package com.omgameserver.engine.lua;

import com.omgameserver.engine.OmgsDispatcher;
import org.luaj.vm2.LuaTable;

public class LuaTesting extends LuaTable {

    private final String FUNCTION_DATA_RECEIVED = "data_received";
    private final String FUNCTION_TICK_RECEIVED = "tick_received";

    public LuaTesting(OmgsDispatcher dispatcher) {
        set(FUNCTION_DATA_RECEIVED, new LuaDataReceivedFunction(dispatcher));
        set(FUNCTION_TICK_RECEIVED, new LuaTickReceivedFunction(dispatcher));
    }
}
