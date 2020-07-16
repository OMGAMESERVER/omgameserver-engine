package com.omgameserver.engine.lua;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.lua.events.LuaOutgoingValueEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaSendFunction extends ThreeArgFunction {

    private final CoreDispatcher dispatcher;

    LuaSendFunction(CoreDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue luaClientUid, LuaValue luaValue, LuaValue luaReliable) {
        Long clientUid = luaClientUid.checklong();
        boolean reliable = luaReliable.checkboolean();
        try {
            dispatcher.dispatch(new LuaOutgoingValueEvent(clientUid, luaValue, reliable));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}
