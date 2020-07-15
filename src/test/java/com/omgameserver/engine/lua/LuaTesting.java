package com.omgameserver.engine.lua;

import com.omgameserver.engine.EngineDispatcher;
import org.luaj.vm2.LuaTable;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public class LuaTesting extends LuaTable {

    private final String FUNCTION_CLIENT_CONNECTED_RECEIVED = "client_connected_received";
    private final String FUNCTION_CLIENT_DISCONNECTED_RECEIVED = "client_disconnected_received";
    private final String FUNCTION_DATA_RECEIVED = "data_received";
    private final String FUNCTION_TICK_RECEIVED = "tick_received";
    private final String FUNCTION_LUA_EVENT_RECEIVED = "lua_event_received";

    public LuaTesting(EngineDispatcher dispatcher) {
        set(FUNCTION_CLIENT_CONNECTED_RECEIVED, new LuaClientConnectedReceivedFunction(dispatcher));
        set(FUNCTION_CLIENT_DISCONNECTED_RECEIVED, new LuaClientDisconnectedReceivedFunction(dispatcher));
        set(FUNCTION_DATA_RECEIVED, new LuaDataReceivedFunction(dispatcher));
        set(FUNCTION_TICK_RECEIVED, new LuaTickReceivedFunction(dispatcher));
        set(FUNCTION_LUA_EVENT_RECEIVED, new LuaEventReceivedFunction(dispatcher));
    }
}
