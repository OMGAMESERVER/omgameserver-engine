package com.omgameserver.engine.lua;

import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.events.LuaClientDisconnectedReceivedEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaClientDisconnectedReceivedFunction extends OneArgFunction {

    private final OmgsDispatcher dispatcher;

    LuaClientDisconnectedReceivedFunction(OmgsDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override public LuaValue call(LuaValue arg) {
        try {
            long clientUid = arg.tolong();
            dispatcher.dispatch(new LuaClientDisconnectedReceivedEvent(clientUid));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}