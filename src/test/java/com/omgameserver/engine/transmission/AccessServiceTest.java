package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.events.*;
import com.omgameserver.engine.utils.TickServiceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public class AccessServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(TickServiceTest.class);

    private AccessService accessService;
    private BlockingQueue<GrantAccessToClient> grantAccessToClients;
    private BlockingQueue<DisconnectClientRequestEvent> disconnectClientRequestEvents;
    private BlockingQueue<AccessKeyExpiredEvent> accessKeyExpiredEvents;
    private ConsumerStub consumerStub;

    @Before
    public void beforeTest() throws UnknownHostException {
        createComponents();
        accessService = new AccessService(properties, executors, dispatcher);
        accessService.postConstruct();
        grantAccessToClients = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        disconnectClientRequestEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        accessKeyExpiredEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @After
    public void afterTest() {
        accessService.finish();
        consumerStub.finish();
    }

    @Test
    public void testGotAccess() throws InterruptedException {
        // Create accessKey
        long accessKey = generateAccessKey();
        dispatcher.dispatch(new AccessKeyCreatedEvent(accessKey));
        // Request access
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        dispatcher.dispatch(new ClientAccessRequestEvent(socketAddress, clientUid, accessKey));
        // Wait event
        GrantAccessToClient event = grantAccessToClients.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Checks
        assertNotNull(event);
        assertEquals(socketAddress, event.getSocketAddress());
        assertEquals(clientUid, event.getClientUid());
    }

    @Test
    public void testAccessForbidden() throws InterruptedException {
        // Create accessKey
        long accessKey = generateAccessKey();
        dispatcher.dispatch(new AccessKeyCreatedEvent(accessKey));
        // Request access
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        dispatcher.dispatch(new ClientAccessRequestEvent(socketAddress, clientUid, accessKey + 1));
        // Wait event
        DisconnectClientRequestEvent event = disconnectClientRequestEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Checks
        assertNotNull(event);
        assertEquals(clientUid, event.getClientUid());
    }

    @Test
    public void testAccessKeyExpire() throws InterruptedException {
        long accessKey = generateAccessKey();
        dispatcher.dispatch(new AccessKeyCreatedEvent(accessKey));
        Thread.sleep(PROPERTY_ACCESS_KEY_LIFETIME);
        // Expiration interval tracked every tick event
        dispatcher.dispatch(new TickEvent(1, 0));
        // Wait event
        AccessKeyExpiredEvent event = accessKeyExpiredEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(event);
        assertEquals(accessKey, event.getAccessKey());
    }

    private class ConsumerStub extends Bolt implements
            GrantAccessToClient.Handler,
            DisconnectClientRequestEvent.Handler,
            AccessKeyExpiredEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleAccessKeyExpired(AccessKeyExpiredEvent event) throws InterruptedException {
            accessKeyExpiredEvents.put(event);
        }

        @Override
        public void handleGrantAccessToClient(GrantAccessToClient event) throws InterruptedException {
            grantAccessToClients.put(event);
        }

        @Override
        public void handleDisconnectClientRequest(DisconnectClientRequestEvent event) throws InterruptedException {
            disconnectClientRequestEvents.put(event);
        }

        void postConstruct() {
            executors.executeInInternalPool(this);
            dispatcher.getDispatcher().subscribe(this, GrantAccessToClient.class);
            dispatcher.getDispatcher().subscribe(this, DisconnectClientRequestEvent.class);
            dispatcher.getDispatcher().subscribe(this, AccessKeyExpiredEvent.class);
        }
    }
}
