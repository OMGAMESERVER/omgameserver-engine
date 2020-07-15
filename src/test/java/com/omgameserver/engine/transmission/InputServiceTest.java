package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.events.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public class InputServiceTest extends BaseServiceTest implements Header {
    static private final Logger logger = LoggerFactory.getLogger(InputServiceTest.class);

    private InputService inputService;
    private ConsumerStub consumerStub;

    private BlockingQueue<IncomingHeaderEvent> incomingHeaderEvents;
    private BlockingQueue<IncomingPayloadEvent> incomingPayloadEvents;
    private BlockingQueue<ClientConnectedEvent> clientConnectedEvents;
    private BlockingQueue<ClientDisconnectedEvent> clientDisconnectedEvents;

    @Before
    public void beforeTest() throws IOException {
        createComponents();
        inputService = new InputService(properties, executors, dispatcher);
        inputService.postConstruct();
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
        incomingHeaderEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        incomingPayloadEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        clientConnectedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        clientDisconnectedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
    }

    @After
    public void afterTest() {
        consumerStub.finish();
        inputService.finish();
    }

    @Test
    public void testInput() throws InterruptedException {
        // Send datagram
        SocketAddress socketAddress = generateSocketAddress();
        String payload = "payload";
        dispatcher.dispatch(createIncomingDatagramEvent(socketAddress,
                1, 2, 3, HEADER_SYS_NOVALUE, payload));
        // Get header event
        IncomingHeaderEvent incomingHeaderEvent =
                incomingHeaderEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        long clientUid = incomingHeaderEvent.getClientUid();
        // Get client connected event
        ClientConnectedEvent clientConnectedEvent = clientConnectedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Get payload event
        IncomingPayloadEvent incomingPayloadEvent =
                incomingPayloadEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Send tick after disconnect interval
        Thread.sleep(PROPERTY_DISCONNECT_INTERVAL * 2);
        dispatcher.dispatch(new TickEvent(1, 0));
        // Get disconnect event
        ClientDisconnectedEvent clientDisconnectedEvent =
                clientDisconnectedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Checks
        assertNotNull(incomingHeaderEvent);
        assertEquals(socketAddress, incomingHeaderEvent.getSocketAddress());
        assertTrue(incomingHeaderEvent.getSeq() == 1);
        assertTrue(incomingHeaderEvent.getAck() == 2);
        assertTrue(incomingHeaderEvent.getBit() == 3);
        assertTrue(incomingHeaderEvent.getSys() == HEADER_SYS_NOVALUE);
        assertNotNull(clientConnectedEvent);
        assertEquals(socketAddress, clientConnectedEvent.getSocketAddress());
        assertEquals(clientUid, clientConnectedEvent.getClientUid());
        assertNotNull(incomingPayloadEvent);
        assertEquals(socketAddress, incomingPayloadEvent.getSocketAddress());
        assertEquals(clientUid, incomingPayloadEvent.getClientUid());
        assertEquals(payload, readPayload(incomingPayloadEvent.getPayload()));
        // Checks
        assertNotNull(clientDisconnectedEvent);
        assertEquals(socketAddress, clientDisconnectedEvent.getSocketAddress());
        assertEquals(clientUid, clientDisconnectedEvent.getClientUid());
    }

    @Test
    public void testDisconnectRequest() throws InterruptedException {
        // Send datagram
        SocketAddress socketAddress = generateSocketAddress();
        String payload = "payload";
        dispatcher.dispatch(createIncomingDatagramEvent(socketAddress,
                1, 2, 3, HEADER_SYS_NOVALUE, payload));
        // Get header event
        IncomingHeaderEvent incomingHeaderEvent =
                incomingHeaderEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        long clientUid = incomingHeaderEvent.getClientUid();
        // Send disconnect request
        dispatcher.dispatch(new DisconnectClientRequestEvent(clientUid));
        // Get client disconnected event
        ClientDisconnectedEvent clientDisconnectedEvent =
                clientDisconnectedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got {}", clientDisconnectedEvent);
        // Asserts
        assertNotNull(clientDisconnectedEvent);
        assertEquals(socketAddress, clientDisconnectedEvent.getSocketAddress());
        assertEquals(clientUid, clientDisconnectedEvent.getClientUid());
    }

    private class ConsumerStub extends Bolt implements
            IncomingHeaderEvent.Handler,
            IncomingPayloadEvent.Handler,
            ClientConnectedEvent.Handler,
            ClientDisconnectedEvent.Handler {

        public ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleIncomingHeader(IncomingHeaderEvent event) throws InterruptedException {
            incomingHeaderEvents.put(event);
        }

        @Override
        public void handleIncomingPayload(IncomingPayloadEvent event) throws InterruptedException {
            incomingPayloadEvents.put(event);
        }

        @Override
        public void handleClientConnected(ClientConnectedEvent event) throws InterruptedException {
            clientConnectedEvents.put(event);
        }

        @Override
        public void handleClientDisconnected(ClientDisconnectedEvent event) throws InterruptedException {
            clientDisconnectedEvents.put(event);
        }

        public void postConstruct() {
            executors.executeInInternalPool(this);
            dispatcher.getDispatcher().subscribe(this, IncomingHeaderEvent.class);
            dispatcher.getDispatcher().subscribe(this, IncomingPayloadEvent.class);
            dispatcher.getDispatcher().subscribe(this, ClientConnectedEvent.class);
            dispatcher.getDispatcher().subscribe(this, ClientDisconnectedEvent.class);
        }
    }
}
