package com.omgameserver.engine.lua;

import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.events.LuaClientConnectedReceivedEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaClientConnectedReceivedFunction extends OneArgFunction {

    private final OmgsDispatcher dispatcher;

    LuaClientConnectedReceivedFunction(OmgsDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override public LuaValue call(LuaValue arg) {
        try {
            long clientUid = arg.tolong();
            dispatcher.getDispatcher().dispatch(new LuaClientConnectedReceivedEvent(clientUid));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}
