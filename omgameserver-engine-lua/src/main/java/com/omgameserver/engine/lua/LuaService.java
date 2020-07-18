package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Event;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.core.events.CoreTickEvent;
import com.omgameserver.engine.lua.events.LuaCustomEvent;
import com.omgameserver.engine.lua.events.LuaDirectEvent;
import org.luaj.vm2.LuaTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static com.omgameserver.engine.lua.LuaEvents.TICK_EVENT_ID;
import static com.omgameserver.engine.lua.LuaTopics.WORKERS_TOPIC;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class LuaService extends Bolt implements
        CoreTickEvent.Handler,
        LuaDirectEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaService.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final LuaWorker defaultWorker;
    private final Map<Long, LuaWorker> routes;

    LuaService(CoreExecutors executors, CoreDispatcher dispatcher, LuaProperties properties,
               LuaGlobalsFactory luaGlobalsFactory) {
        super("lua", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        defaultWorker = new LuaWorker(executors, dispatcher, properties, luaGlobalsFactory, properties.getMainScript());
        defaultWorker.postConstruct();
        routes = new HashMap<>();
    }

    @Override
    public void handleCoreTick(CoreTickEvent event) throws InterruptedException {
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", TICK_EVENT_ID);
        luaEvent.set("tick_number", event.getNumber());
        luaEvent.set("delta_time", event.getDeltaTime());
        dispatcher.dispatch(new LuaCustomEvent(TICK_EVENT_ID, luaEvent), WORKERS_TOPIC);
    }

    @Override
    public void handleLuaDirectEvent(LuaDirectEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long uid = event.getUid();
        Event eventToRoute = event.getEvent();
        // Routing event to worker
        LuaWorker targetWorker = routes.getOrDefault(uid, defaultWorker);
        dispatcher.dispatch(eventToRoute, targetWorker);
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, CoreTickEvent.class);
        dispatcher.getDispatcher().subscribe(this, LuaDirectEvent.class);
    }
}
