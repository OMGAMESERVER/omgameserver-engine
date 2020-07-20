package com.omgameserver.engine.lua.runtime;

import com.omgameserver.engine.core.CoreDispatcher;
import org.luaj.vm2.LuaTable;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaTesting extends LuaTable {

    private final String CONNECTED_RECEIVED_FUNCTION = "connected_received";
    private final String DISCONNECTED_RECEIVED_FUNCTION = "disconnected_received";
    private final String DATA_RECEIVED_FUNCTION = "data_received";
    private final String TICK_RECEIVED_FUNCTION = "tick_received";
    private final String CUSTOM_EVENT_RECEIVED_FUNCTION = "custom_event_received";

    LuaTesting(CoreDispatcher dispatcher) {
        set(CONNECTED_RECEIVED_FUNCTION, new LuaConnectedReceivedFunction(dispatcher));
        set(DISCONNECTED_RECEIVED_FUNCTION, new LuaDisconnectedReceivedFunction(dispatcher));
        set(DATA_RECEIVED_FUNCTION, new LuaDataReceivedFunction(dispatcher));
        set(TICK_RECEIVED_FUNCTION, new LuaTickReceivedFunction(dispatcher));
        set(CUSTOM_EVENT_RECEIVED_FUNCTION, new LuaCustomEventReceivedFunction(dispatcher));
    }
}