package com.omgameserver.engine.lua;

import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.events.DisconnectClientRequestEvent;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaDisconnectClientFunction extends OneArgFunction {
    static private final Logger logger = LoggerFactory.getLogger(LuaDisconnectClientFunction.class);

    private final OmgsDispatcher dispatcher;

    LuaDisconnectClientFunction(OmgsDispatcher dispatcher) {
        super();
        this.dispatcher = dispatcher;
    }

    @Override
    public LuaValue call(LuaValue arg) {
        if (arg.islong()) {
            long clientUid = arg.tolong();
            try {
                dispatcher.getDispatcher().dispatch(new DisconnectClientRequestEvent(clientUid));
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
