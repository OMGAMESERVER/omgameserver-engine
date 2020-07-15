package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.EngineDispatcher;
import com.omgameserver.engine.EngineExecutors;
import com.omgameserver.engine.EngineProperties;
import com.omgameserver.engine.events.*;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaWorker extends Bolt implements
        ClientConnectedEvent.Handler,
        ClientDisconnectedEvent.Handler,
        IncomingLuaValueEvent.Handler,
        LuaEvent.Handler,
        TickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorker.class);

    private final EngineExecutors executors;
    private final EngineDispatcher dispatcher;
    private final LuaGlobals luaGlobals;
    private final LuaEngine luaEngine;

    private final String EVENT_CLIENT_CONNECTED = "client_connected";
    private final String EVENT_CLIENT_DISCONNECTED = "client_disconnected";
    private final String EVENT_RECEIVED = "received";
    private final String EVENT_TICK = "tick";

    LuaWorker(EngineProperties properties, EngineExecutors executors, EngineDispatcher dispatcher,
              LuaGlobals luaGlobals, String luaScript) {
        super(luaScript, properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.luaGlobals = luaGlobals;
        Globals globals = luaGlobals.getGlobals();
        luaEngine = new LuaEngine(dispatcher, globals);
        globals.set("engine", luaEngine);
        globals.loadfile(luaScript).call();
    }

    @Override
    public void handleClientConnected(ClientConnectedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_CLIENT_CONNECTED);
        luaEvent.set("client_uid", event.getClientUid());
        luaEngine.dispatch(EVENT_CLIENT_CONNECTED, luaEvent);
    }

    @Override
    public void handleClientDisconnected(ClientDisconnectedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_CLIENT_DISCONNECTED);
        luaEvent.set("client_uid", event.getClientUid());
        luaEngine.dispatch(EVENT_CLIENT_DISCONNECTED, luaEvent);
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
        luaEngine.dispatch(EVENT_RECEIVED, luaEvent);
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
        luaEngine.dispatch(EVENT_TICK, luaEvent);
    }

    @Override
    public void handleLuaEvent(LuaEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        String eventId = event.getId();
        LuaValue luaEvent = event.getEvent();
        luaEngine.dispatch(eventId, luaEvent);
        if (logger.isDebugEnabled()) {
            logger.debug("Event with id={} was dispatched", eventId);
        }
    }

    void postConstruct() {
        executors.executeInUserPool(this);
        dispatcher.getDispatcher().subscribe(this, ClientConnectedEvent.class);
        dispatcher.getDispatcher().subscribe(this, ClientDisconnectedEvent.class);
        dispatcher.getDispatcher().subscribe(this, TickEvent.class);
        dispatcher.getDispatcher().subscribe(this, LuaEvent.class);
        // Subscribe to all event dispatched directly to this bolt
        dispatcher.getDispatcher().subscribe(this);
    }
}
