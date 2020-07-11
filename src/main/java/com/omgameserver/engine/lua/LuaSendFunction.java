package com.omgameserver.engine.lua;

import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.events.OutgoingLuaValueEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;

class LuaSendFunction extends ThreeArgFunction {

    private final OmgsDispatcher dispatcher;

    LuaSendFunction(OmgsDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue luaClientUid, LuaValue luaValue, LuaValue luaReliable) {
        Long clientUid = luaClientUid.checklong();
        boolean reliable = luaReliable.checkboolean();
        try {
            dispatcher.getDispatcher().dispatch(new OutgoingLuaValueEvent(clientUid, luaValue, reliable));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}