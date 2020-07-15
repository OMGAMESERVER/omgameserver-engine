package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.EngineDispatcher;
import com.omgameserver.engine.EngineExecutors;
import com.omgameserver.engine.EngineProperties;
import com.omgameserver.engine.events.IncomingLuaValueEvent;
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
        IncomingLuaValueEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaService.class);

    private final EngineProperties properties;
    private final EngineExecutors executors;
    private final EngineDispatcher dispatcher;
    private final LuaWorker defaultWorker;
    private final Map<Long, LuaWorker> routes;

    LuaService(EngineProperties properties, EngineExecutors executors, EngineDispatcher dispatcher,
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
        dispatcher.dispatch(event, targetWorker);
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, IncomingLuaValueEvent.class);
    }
}
