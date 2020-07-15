package com.omgameserver.engine.lua;

import com.omgameserver.engine.EngineDispatcher;
import com.omgameserver.engine.events.LuaClientDisconnectedReceivedEvent;
import com.omgameserver.engine.events.LuaEventReceivedEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaEventReceivedFunction extends TwoArgFunction {

    private final EngineDispatcher dispatcher;

    LuaEventReceivedFunction(EngineDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        if (arg1.isstring() && arg2.isstring()) {
            try {
                String eventId = arg1.tojstring();
                String data = arg2.tojstring();
                dispatcher.dispatch(new LuaEventReceivedEvent(eventId, data));
                return LuaBoolean.TRUE;
            } catch (InterruptedException e) {
                return LuaBoolean.FALSE;
            }
        } else {
            return LuaBoolean.FALSE;
        }
    }
}
