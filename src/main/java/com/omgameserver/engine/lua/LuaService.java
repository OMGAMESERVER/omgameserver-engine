package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsExecutors;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.IncomingLuaValueEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class LuaService extends Bolt implements
        IncomingLuaValueEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaService.class);

    private final OmgsProperties properties;
    private final OmgsExecutors executors;
    private final Dispatcher dispatcher;
    private final ThreadPoolTaskExecutor workerExecutor;
    private final LuaWorker defaultWorker;
    private final Map<Long, LuaWorker> routes;

    LuaService(OmgsProperties properties, OmgsExecutors executors, Dispatcher dispatcher,
               LuaGlobals luaGlobals) {
        super("lua", properties.getQueueSize());
        this.properties = properties;
        this.executors = executors;
        this.dispatcher = dispatcher;
        workerExecutor = createWorkerExecutor();
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
        dispatcher.subscribe(this, IncomingLuaValueEvent.class);
    }

    private ThreadPoolTaskExecutor createWorkerExecutor() {
        ThreadPoolTaskExecutor workerExecutor = new ThreadPoolTaskExecutor();
        workerExecutor.setThreadNamePrefix("omgs-");
        workerExecutor.setCorePoolSize(properties.getThreadPoolSize());
        workerExecutor.initialize();
        logger.info("Worker pool with size={} created", properties.getThreadPoolSize());
        return workerExecutor;
    }
}
