package com.omgameserver.engine.lua;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.lua.events.LuaTickReceivedEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaTickReceivedFunction extends TwoArgFunction {

    private final CoreDispatcher dispatcher;

    LuaTickReceivedFunction(CoreDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        try {
            long number = arg1.tolong();
            long deltaTime = arg2.tolong();
            dispatcher.dispatch(new LuaTickReceivedEvent(number, deltaTime));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}
