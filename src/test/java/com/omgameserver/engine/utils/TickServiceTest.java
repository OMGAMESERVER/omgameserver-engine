package com.omgameserver.engine.utils;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.events.TickEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TickServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(TickServiceTest.class);

    private TickService tickService;
    private BlockingQueue<TickEvent> tickEvents;
    private ConsumerStub consumerStub;

    @Before
    public void beforeTest() throws UnknownHostException {
        createComponents();
        tickService = new TickService(properties, dispatcher, threadPoolTaskExecutor);
        tickService.postConstruct();
        tickEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @After
    public void afterTest() {
        tickService.finish();
        consumerStub.finish();
    }

    @Test
    public void testTickEvents() throws InterruptedException {
        TickEvent tick1 = tickEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        TickEvent tick2 = tickEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        TickEvent tick3 = tickEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(tick1);
        assertNotNull(tick2);
        assertNotNull(tick3);
    }

    private class ConsumerStub extends Bolt implements TickEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleTick(TickEvent event) throws InterruptedException {
            tickEvents.put(event);
        }

        void postConstruct() {
            threadPoolTaskExecutor.execute(this);
            dispatcher.subscribe(this, TickEvent.class);
        }
    }
}
