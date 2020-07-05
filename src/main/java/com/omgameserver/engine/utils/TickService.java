package com.omgameserver.engine.utils;

import com.crionuke.bolts.Dispatcher;
import com.crionuke.bolts.Worker;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
class TickService extends Worker {
    static private final Logger logger = LoggerFactory.getLogger(TickService.class);

    private final OmgsProperties properties;
    private final Dispatcher dispatcher;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    TickService(OmgsProperties properties, Dispatcher dispatcher, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
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
        threadPoolTaskExecutor.execute(this);
    }
}
