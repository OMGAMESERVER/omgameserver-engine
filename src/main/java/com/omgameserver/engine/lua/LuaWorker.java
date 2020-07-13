package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.OmgsExecutors;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.ClientConnectedEvent;
import com.omgameserver.engine.events.ClientDisconnectedEvent;
import com.omgameserver.engine.events.IncomingLuaValueEvent;
import com.omgameserver.engine.events.TickEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
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
        TickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorker.class);

    private final OmgsExecutors executors;
    private final OmgsDispatcher dispatcher;
    private final LuaGlobals luaGlobals;
    private final LuaRuntime luaRuntime;

    private final String EVENT_CLIENT_CONNTECTED = "client_connected";
    private final String EVENT_CLIENT_DISCONNECTED = "client_disconnected";
    private final String EVENT_RECEIVED = "received";
    private final String EVENT_TICK = "tick";

    LuaWorker(OmgsProperties properties, OmgsExecutors executors, OmgsDispatcher dispatcher,
              LuaGlobals luaGlobals, String luaScript) {
        super(luaScript, properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.luaGlobals = luaGlobals;
        Globals globals = luaGlobals.getGlobals();
        luaRuntime = new LuaRuntime(dispatcher, globals);
        globals.set("runtime", luaRuntime);
        globals.loadfile(luaScript).call();
    }

    @Override
    public void handleClientConnected(ClientConnectedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_CLIENT_CONNTECTED);
        luaEvent.set("client_uid", event.getClientUid());
        luaRuntime.dispatch(EVENT_CLIENT_CONNTECTED, luaEvent);
    }

    @Override
    public void handleClientDisconnected(ClientDisconnectedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_CLIENT_DISCONNECTED);
        luaEvent.set("client_uid", event.getClientUid());
        luaRuntime.dispatch(EVENT_CLIENT_DISCONNECTED, luaEvent);
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
        executors.executeInUserPool(this);
        dispatcher.getDispatcher().subscribe(this, ClientConnectedEvent.class);
        dispatcher.getDispatcher().subscribe(this, ClientDisconnectedEvent.class);
        dispatcher.getDispatcher().subscribe(this, TickEvent.class);
        // Subscribe to all event dispatched directly to this bolt
        dispatcher.getDispatcher().subscribe(this);
    }
}
