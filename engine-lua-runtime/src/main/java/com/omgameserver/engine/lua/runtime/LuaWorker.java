package com.omgameserver.engine.lua.runtime;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.lua.runtime.events.LuaCustomEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.omgameserver.engine.lua.runtime.LuaEvents.CORE_TICK_EVENT_ID;
import static com.omgameserver.engine.lua.runtime.LuaTopics.WORKERS_TOPIC;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaWorker extends Bolt implements LuaCustomEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorker.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final LuaRuntime luaRuntime;

    LuaWorker(CoreExecutors executors, CoreDispatcher dispatcher, LuaProperties properties,
              LuaGlobalsFactory luaGlobalsFactory, String luaScript) {
        super(luaScript, properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        Globals globals = luaGlobalsFactory.createGlobals();
        luaRuntime = new LuaRuntime(dispatcher, globals);
        globals.set("runtime", luaRuntime);
        globals.loadfile(luaScript).call();
    }

    @Override
    public void handleLuaCustomEvent(LuaCustomEvent event) {
        if (logger.isTraceEnabled()) {
            if (event.getEventId() != CORE_TICK_EVENT_ID) {
                logger.trace("Handle {}", event);
            }
        }
        String eventId = event.getEventId();
        LuaValue luaEvent = event.getLuaEvent();
        luaRuntime.dispatch(eventId, luaEvent);
    }

    void postConstruct() {
        executors.executeInUserPool(this);
        dispatcher.getDispatcher().subscribe(this, WORKERS_TOPIC);
        // Subscribe to all events dispatched directly to this bolt
        dispatcher.getDispatcher().subscribe(this);
    }
}
