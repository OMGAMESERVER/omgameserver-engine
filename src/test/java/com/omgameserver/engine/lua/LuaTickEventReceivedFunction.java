package com.omgameserver.engine.lua;

import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.events.LuaTickEventReceived;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

class LuaTickEventReceivedFunction extends TwoArgFunction {

    private final Dispatcher dispatcher;

    LuaTickEventReceivedFunction(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        try {
            dispatcher.dispatch(new LuaTickEventReceived(arg1.tolong(), arg2.tolong()));
            return LuaBoolean.TRUE;
        } catch (InterruptedException e) {
            return LuaBoolean.FALSE;
        }
    }
}
