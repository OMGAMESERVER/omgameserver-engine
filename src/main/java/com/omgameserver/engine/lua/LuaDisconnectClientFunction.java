package com.omgameserver.engine.lua;

import com.omgameserver.engine.EngineDispatcher;
import com.omgameserver.engine.events.DisconnectClientRequestEvent;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaDisconnectClientFunction extends OneArgFunction {
    static private final Logger logger = LoggerFactory.getLogger(LuaDisconnectClientFunction.class);

    private final EngineDispatcher dispatcher;

    LuaDisconnectClientFunction(EngineDispatcher dispatcher) {
        super();
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg) {
        if (arg.islong()) {
            long clientUid = arg.tolong();
            try {
                dispatcher.dispatch(new DisconnectClientRequestEvent(clientUid));
                if (logger.isDebugEnabled()) {
                    logger.debug("Request to disconnect client with uid={}", clientUid);
                }
                return LuaBoolean.TRUE;
            } catch (InterruptedException e) {
                return LuaBoolean.FALSE;
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Wrong clientUid value {}", arg);
            }
            return LuaBoolean.FALSE;
        }
    }
}
