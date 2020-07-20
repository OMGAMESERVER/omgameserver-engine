package com.omgameserver.engine.core;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.events.CoreTickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class CoreTickServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(CoreTickServiceTest.class);

    private CoreTickService coreTickService;
    private BlockingQueue<CoreTickEvent> tickEvents;
    private ConsumerStub consumerStub;

    @BeforeEach
    public void beforeEach() throws UnknownHostException {
        createComponents();
        coreTickService = new CoreTickService(coreProperties, coreDispatcher, coreExecutors);
        coreTickService.postConstruct();
        tickEvents = new LinkedBlockingQueue<>(QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @AfterEach
    public void afterEach() {
        coreTickService.finish();
        consumerStub.finish();
    }

    @Test
    public void testTickEvents() throws InterruptedException {
        CoreTickEvent tick1 = tickEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        CoreTickEvent tick2 = tickEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        CoreTickEvent tick3 = tickEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(tick1);
        assertNotNull(tick2);
        assertNotNull(tick3);
    }

    private class ConsumerStub extends Bolt implements CoreTickEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", QUEUE_SIZE);
        }

        @Override
        public void handleCoreTick(CoreTickEvent event) throws InterruptedException {
            tickEvents.put(event);
        }

        void postConstruct() {
            coreExecutors.executeInInternalPool(this);
            coreDispatcher.getDispatcher().subscribe(this, CoreTickEvent.class);
        }
    }
}
