package com.omgameserver.engine.lua;

import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.events.LuaTickReceivedEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

class LuaTickReceivedFunction extends TwoArgFunction {

    private final OmgsDispatcher dispatcher;

    LuaTickReceivedFunction(OmgsDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        try {
            long number = arg1.tolong();
            long deltaTime = arg2.tolong();
            dispatcher.getDispatcher().dispatch(new LuaTickReceivedEvent(number, deltaTime));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}
