package com.omgameserver.engine.lua;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.lua.events.LuaCustomEventReceivedEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaCustomEventReceivedFunction extends TwoArgFunction {

    private final CoreDispatcher dispatcher;

    LuaCustomEventReceivedFunction(CoreDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        if (arg1.isstring() && arg2.isstring()) {
            try {
                String eventId = arg1.tojstring();
                String data = arg2.tojstring();
                dispatcher.dispatch(new LuaCustomEventReceivedEvent(eventId, data));
                return LuaBoolean.TRUE;
            } catch (InterruptedException e) {
                return LuaBoolean.FALSE;
            }
        } else {
            return LuaBoolean.FALSE;
        }
    }
}
