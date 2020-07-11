package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.OmgsExecutors;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.IncomingLuaValueEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
class LuaService extends Bolt implements
        IncomingLuaValueEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaService.class);

    private final OmgsProperties properties;
    private final OmgsExecutors executors;
    private final OmgsDispatcher dispatcher;
    private final LuaWorker defaultWorker;
    private final Map<Long, LuaWorker> routes;

    LuaService(OmgsProperties properties, OmgsExecutors executors, OmgsDispatcher dispatcher,
               LuaGlobals luaGlobals) {
        super("lua", properties.getQueueSize());
        this.properties = properties;
        this.executors = executors;
        this.dispatcher = dispatcher;
        defaultWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals, properties.getMainScript());
        defaultWorker.postConstruct();
        routes = new HashMap<>();
    }

    @Override
    public void handleIncomingLuaValue(IncomingLuaValueEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        // Routing event to worker
        LuaWorker targetWorker = routes.getOrDefault(clientUid, defaultWorker);
        dispatcher.getDispatcher().dispatch(event, targetWorker);
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, IncomingLuaValueEvent.class);
    }
}
