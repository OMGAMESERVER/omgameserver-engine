package com.omgameserver.engine.utils;

import com.crionuke.bolts.Worker;
import com.omgameserver.engine.EngineDispatcher;
import com.omgameserver.engine.EngineExecutors;
import com.omgameserver.engine.EngineProperties;
import com.omgameserver.engine.events.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class TickService extends Worker {
    static private final Logger logger = LoggerFactory.getLogger(TickService.class);

    private final EngineProperties properties;
    private final EngineDispatcher dispatcher;
    private final EngineExecutors executors;

    TickService(EngineProperties properties, EngineDispatcher dispatcher, EngineExecutors executors) {
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.executors = executors;
    }

    @Override
    public void run() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("tick-" + uid);
        logger.debug("{} started", this);
        long number = 0;
        long lastTime = System.currentTimeMillis();
        looping = true;
        try {
            while (looping) {
                number++;
                dispatcher.dispatch(new TickEvent(number, System.currentTimeMillis() - lastTime));
                lastTime = System.currentTimeMillis();
                Thread.sleep(properties.getTickInterval());
            }
        } catch (InterruptedException ie) {
            logger.debug("{} interrupted", this);
            looping = false;
        }
        logger.debug("{} finished", this);
        Thread.currentThread().setName(oldThreadName);
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
    }
}
