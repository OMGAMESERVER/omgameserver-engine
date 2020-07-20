package com.omgameserver.engine.core;

import com.crionuke.bolts.Worker;
import com.omgameserver.engine.core.events.CoreTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class CoreTickService extends Worker {
    static private final Logger logger = LoggerFactory.getLogger(CoreTickService.class);

    private final CoreProperties properties;
    private final CoreDispatcher dispatcher;
    private final CoreExecutors executors;

    CoreTickService(CoreProperties properties, CoreDispatcher dispatcher, CoreExecutors executors) {
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
                dispatcher.dispatch(new CoreTickEvent(number, System.currentTimeMillis() - lastTime));
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
