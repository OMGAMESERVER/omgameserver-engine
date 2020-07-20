package com.omgameserver.engine.lua.runtime;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.lua.runtime.events.LuaDataReceivedEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaDataReceivedFunction extends TwoArgFunction {

    private final CoreDispatcher dispatcher;

    LuaDataReceivedFunction(CoreDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        try {
            long clientUid = arg1.tolong();
            String data = arg2.tojstring();
            dispatcher.dispatch(new LuaDataReceivedEvent(clientUid, data));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}
