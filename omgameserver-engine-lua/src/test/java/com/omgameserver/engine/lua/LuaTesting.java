package com.omgameserver.engine.lua;

import com.omgameserver.engine.core.CoreDispatcher;
import org.luaj.vm2.LuaTable;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaTesting extends LuaTable {

    private final String CLIENT_CONNECTED_RECEIVED_FUNCTION = "client_connected_received";
    private final String CLIENT_DISCONNECTED_RECEIVED_FUNCTION = "client_disconnected_received";
    private final String DATA_RECEIVED_FUNCTION = "data_received";
    private final String TICK_RECEIVED_FUNCTION = "tick_received";
    private final String CUSTOM_EVENT_RECEIVED_FUNCTION = "custom_event_received";

    LuaTesting(CoreDispatcher dispatcher) {
        set(CLIENT_CONNECTED_RECEIVED_FUNCTION, new LuaConnectedReceivedFunction(dispatcher));
        set(CLIENT_DISCONNECTED_RECEIVED_FUNCTION, new LuaDisconnectedReceivedFunction(dispatcher));
        set(DATA_RECEIVED_FUNCTION, new LuaDataReceivedFunction(dispatcher));
        set(TICK_RECEIVED_FUNCTION, new LuaTickReceivedFunction(dispatcher));
        set(CUSTOM_EVENT_RECEIVED_FUNCTION, new LuaCustomEventReceivedFunction(dispatcher));
    }
}