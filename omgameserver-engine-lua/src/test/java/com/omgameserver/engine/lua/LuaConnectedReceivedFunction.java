package com.omgameserver.engine.lua;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.lua.events.LuaConnectedReceivedEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaConnectedReceivedFunction extends TwoArgFunction {

    private final CoreDispatcher dispatcher;

    LuaConnectedReceivedFunction(CoreDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        try {
            long clientUid = arg1.tolong();
            String clientType = arg2.tojstring();
            dispatcher.dispatch(new LuaConnectedReceivedEvent(clientUid, clientType));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}
