package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.core.events.CoreTickEvent;
import com.omgameserver.engine.lua.events.LuaCustomEvent;
import com.omgameserver.engine.lua.events.LuaIncomingValueEvent;
import com.omgameserver.engine.udp.events.UdpClientConnectedEvent;
import com.omgameserver.engine.udp.events.UdpClientDisconnectedEvent;
import org.luaj.vm2.LuaTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class LuaService extends Bolt implements
        CoreTickEvent.Handler,
        UdpClientConnectedEvent.Handler,
        UdpClientDisconnectedEvent.Handler,
        LuaIncomingValueEvent.Handler, LuaEventConstants {
    static private final Logger logger = LoggerFactory.getLogger(LuaService.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final LuaWorker defaultWorker;
    private final Map<Long, LuaWorker> routes;

    LuaService(CoreExecutors executors, CoreDispatcher dispatcher, LuaProperties properties,
               LuaGlobals luaGlobals) {
        super("lua", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        defaultWorker = new LuaWorker(executors, dispatcher, properties, luaGlobals, properties.getMainScript());
        defaultWorker.postConstruct();
        routes = new HashMap<>();
    }

    @Override
    public void handleCoreTick(CoreTickEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_TICK_ID);
        luaEvent.set("tick_number", event.getNumber());
        luaEvent.set("delta_time", event.getDeltaTime());
        dispatcher.dispatch(new LuaCustomEvent(EVENT_TICK_ID, luaEvent));
    }

    @Override
    public void handleUdpClientConnected(UdpClientConnectedEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_CONNECTED_ID);
        luaEvent.set("client_uid", event.getClientUid());
        luaEvent.set("client_type", UDP_CLIENT_TYPE);
        dispatcher.dispatch(new LuaCustomEvent(EVENT_CONNECTED_ID, luaEvent));
    }

    @Override
    public void handleUdpClientDisconnected(UdpClientDisconnectedEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_DISCONNECTED_ID);
        luaEvent.set("client_uid", event.getClientUid());
        luaEvent.set("client_type", UDP_CLIENT_TYPE);
        dispatcher.dispatch(new LuaCustomEvent(EVENT_DISCONNECTED_ID, luaEvent));
    }

    @Override
    public void handleLuaIncoming(LuaIncomingValueEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", EVENT_RECEIVED_ID);
        luaEvent.set("client_uid", clientUid);
        luaEvent.set("data", event.getLuaValue());
        // Routing event to worker
        LuaWorker targetWorker = routes.getOrDefault(clientUid, defaultWorker);
        dispatcher.dispatch(new LuaCustomEvent(EVENT_RECEIVED_ID, luaEvent), targetWorker);
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, CoreTickEvent.class);
        dispatcher.getDispatcher().subscribe(this, UdpClientConnectedEvent.class);
        dispatcher.getDispatcher().subscribe(this, UdpClientDisconnectedEvent.class);
        dispatcher.getDispatcher().subscribe(this, LuaIncomingValueEvent.class);
    }
}
