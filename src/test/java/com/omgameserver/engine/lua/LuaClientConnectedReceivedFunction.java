package com.omgameserver.engine.lua;

import com.omgameserver.engine.EngineDispatcher;
import com.omgameserver.engine.events.LuaClientConnectedReceivedEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaClientConnectedReceivedFunction extends OneArgFunction {

    private final EngineDispatcher dispatcher;

    LuaClientConnectedReceivedFunction(EngineDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override public LuaValue call(LuaValue arg) {
        try {
            long clientUid = arg.tolong();
            dispatcher.dispatch(new LuaClientConnectedReceivedEvent(clientUid));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}
