package com.omgameserver.engine.utils;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.events.SecretKeyCreatedEvent;
import com.omgameserver.engine.events.SecretKeyExpiredEvent;
import com.omgameserver.engine.events.TickEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class KeysTrackerServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(TickServiceTest.class);

    private KeysTrackerService keysTrackerService;
    private BlockingQueue<SecretKeyExpiredEvent> secretKeyExpiredEvents;
    private ConsumerStub consumerStub;

    @Before
    public void beforeTest() throws UnknownHostException {
        createComponents();
        keysTrackerService = new KeysTrackerService(properties, executors, dispatcher);
        keysTrackerService.postConstruct();
        secretKeyExpiredEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @After
    public void afterTest() {
        keysTrackerService.finish();
        consumerStub.finish();
    }

    @Test
    public void testSecretKeyExpire() throws NoSuchAlgorithmException, InterruptedException {
        long keyUid = 1;
        SecretKey secretKey = createSecretKey();
        dispatcher.getDispatcher().dispatch(new SecretKeyCreatedEvent(keyUid, secretKey));
        Thread.sleep(PROPERTY_SECRET_KEY_LIFETIME);
        // Expiration interval tracked every tick event
        dispatcher.getDispatcher().dispatch(new TickEvent(1, 0));
        // Wait event
        SecretKeyExpiredEvent expiredEvent = secretKeyExpiredEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(expiredEvent);
        assertEquals(keyUid, expiredEvent.getKeyUid());
    }

    private class ConsumerStub extends Bolt implements SecretKeyExpiredEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleSecretKeyExpired(SecretKeyExpiredEvent event) throws InterruptedException {
            secretKeyExpiredEvents.put(event);
        }

        void postConstruct() {
            executors.executeInInternalPool(this);
            dispatcher.getDispatcher().subscribe(this, SecretKeyExpiredEvent.class);
        }
    }
}
