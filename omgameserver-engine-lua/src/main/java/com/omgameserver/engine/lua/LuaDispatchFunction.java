package com.omgameserver.engine.lua;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.lua.events.LuaCustomEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaDispatchFunction extends TwoArgFunction {
    static private final Logger logger = LoggerFactory.getLogger(LuaDispatchFunction.class);

    private final CoreDispatcher dispatcher;

    LuaDispatchFunction(CoreDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        if (arg1.isstring()) {
            String eventId = arg1.tojstring();
            try {
                dispatcher.dispatch(new LuaCustomEvent(eventId, arg2));
                return LuaBoolean.TRUE;
            } catch (InterruptedException e) {
                return LuaBoolean.FALSE;
            }
        } else {
            return LuaBoolean.FALSE;
        }
    }
}
