package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.IncomingLuaValueEvent;
import com.omgameserver.engine.events.TickEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class LuaWorker extends Bolt implements
        IncomingLuaValueEvent.Handler,
        TickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorker.class);

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;
    private final Globals globals;
    private final LuaRuntime luaRuntime;

    private final String EVENT_RECEIVED = "received";
    private final String EVENT_TICK = "tick";

    LuaWorker(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher,
              String luaScript) {
        super(luaScript, properties.getQueueSize());
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
        globals = JsePlatform.standardGlobals();
        globals.finder = new LuaScriptFinder();
        luaRuntime = new LuaRuntime(globals);
        globals.set("runtime", luaRuntime);
        globals.loadfile(luaScript).call();
    }

    @Override
    public void handleIncomingLuaValue(IncomingLuaValueEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_RECEIVED);
        luaEvent.set("client_uid", event.getClientUid());
        luaEvent.set("data", event.getLuaValue());
        luaRuntime.dispatch(EVENT_RECEIVED, luaEvent);
    }

    @Override
    public void handleTick(TickEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_TICK);
        luaEvent.set("tick_number", event.getNumber());
        luaEvent.set("delta_time", event.getDeltaTime());
        luaRuntime.dispatch(EVENT_TICK, luaEvent);
    }

    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, TickEvent.class);
        // Subscribe to all event dispatched directly to this bolt
        dispatcher.subscribe(this);
    }
}
