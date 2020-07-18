package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.lua.events.LuaCustomEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaWorker extends Bolt implements LuaCustomEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorker.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final LuaEngine luaEngine;

    LuaWorker(CoreExecutors executors, CoreDispatcher dispatcher, LuaProperties properties,
              LuaGlobalsFactory luaGlobalsFactory, String luaScript) {
        super(luaScript, properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        Globals globals = luaGlobalsFactory.createGlobals();
        luaEngine = new LuaEngine(dispatcher, globals);
        globals.set("engine", luaEngine);
        globals.loadfile(luaScript).call();
    }

    @Override
    public void handleLuaCustomEvent(LuaCustomEvent event) {
        if (logger.isTraceEnabled()) {
            if (event.getId() != LuaEventConstants.TICK_EVENT_ID) {
                logger.trace("Handle {}", event);
            }
        }
        String eventId = event.getId();
        LuaValue luaEvent = event.getEvent();
        luaEngine.dispatch(eventId, luaEvent);
    }

    void postConstruct() {
        executors.executeInUserPool(this);
        dispatcher.getDispatcher().subscribe(this, LuaCustomEvent.class);
        // Subscribe to all events dispatched directly to this bolt
        dispatcher.getDispatcher().subscribe(this);
    }
}
