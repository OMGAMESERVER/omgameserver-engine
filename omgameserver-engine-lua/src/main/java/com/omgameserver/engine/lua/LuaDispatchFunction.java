package com.omgameserver.engine.lua;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.lua.events.LuaCustomEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaDispatchFunction extends ThreeArgFunction {
    static private final Logger logger = LoggerFactory.getLogger(LuaDispatchFunction.class);

    private final CoreDispatcher dispatcher;

    LuaDispatchFunction(CoreDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue luaTopic, LuaValue luaEventId, LuaValue luaValue) {
        if (luaTopic != NIL && luaEventId != NIL && luaValue != NIL) {
            try {
                String topic = luaTopic.checkjstring();
                String eventId = luaEventId.checkjstring();
                dispatcher.dispatch(new LuaCustomEvent(eventId, luaValue), topic);
                return LuaBoolean.TRUE;
            } catch (LuaError e) {
                return LuaBoolean.FALSE;
            } catch (InterruptedException e) {
                return LuaBoolean.FALSE;
            }
        } else {
            return LuaBoolean.FALSE;
        }
    }
}
