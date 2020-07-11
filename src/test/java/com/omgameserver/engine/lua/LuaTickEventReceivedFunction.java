package com.omgameserver.engine.lua;

import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.events.LuaTickEventReceived;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

class LuaTickEventReceivedFunction extends TwoArgFunction {

    private final OmgsDispatcher dispatcher;

    LuaTickEventReceivedFunction(OmgsDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        try {
            dispatcher.getDispatcher().dispatch(new LuaTickEventReceived(arg1.tolong(), arg2.tolong()));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}
