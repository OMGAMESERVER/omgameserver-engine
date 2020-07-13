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
    private BlockingQueue<ClientAccessRequestEvent> clientAccessRequestEvents;
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
        clientAccessRequestEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
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
    public void testHeaderSplit() throws InterruptedException {
        // Send datagram
        long accessKey = generateAccessKey();
        SocketAddress socketAddress = generateSocketAddress();
        dispatcher.getDispatcher().dispatch(createAccessRequestDatagram(socketAddress,
                1, 2, 3, HEADER_SYS_NOVALUE, accessKey));
        // Waiting header event
        IncomingHeaderEvent incomingHeaderEvent = incomingHeaderEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Checks
        assertNotNull(incomingHeaderEvent);
        assertEquals(socketAddress, incomingHeaderEvent.getSocketAddress());
        assertTrue(incomingHeaderEvent.getSeq() == 1);
        assertTrue(incomingHeaderEvent.getAck() == 2);
        assertTrue(incomingHeaderEvent.getBit() == 3);
        assertTrue(incomingHeaderEvent.getSys() == HEADER_SYS_NOVALUE);
    }

    @Test
    public void testAccessRequest() throws InterruptedException {
        // Send datagram
        long accessKey = generateAccessKey();
        SocketAddress socketAddress = generateSocketAddress();
        dispatcher.getDispatcher().dispatch(createAccessRequestDatagram(socketAddress,
                1, 2, 3, HEADER_SYS_NOVALUE, accessKey));
        // Wait result event
        ClientAccessRequestEvent clientAccessRequestEvent =
                clientAccessRequestEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Checks
        assertNotNull(clientAccessRequestEvent);
        assertEquals(socketAddress, clientAccessRequestEvent.getSocketAddress());
        assertEquals(accessKey, clientAccessRequestEvent.getAccessKey());
    }


    @Test
    public void testGrantAccessAndPayloadSplit() throws InterruptedException {
        // Send datagram
        long accessKey = generateAccessKey();
        SocketAddress socketAddress = generateSocketAddress();
        dispatcher.getDispatcher().dispatch(createAccessRequestDatagram(socketAddress,
                1, 2, 3, HEADER_SYS_NOVALUE, accessKey));
        // As result get header and access request events
        IncomingHeaderEvent incomingHeaderEvent =
                incomingHeaderEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        ClientAccessRequestEvent clientAccessRequestEvent =
                clientAccessRequestEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        long clientUid = clientAccessRequestEvent.getClientUid();
        // Grant access to client
        dispatcher.getDispatcher().dispatch(new GrantAccessToClient(socketAddress, clientUid));
        // Waiting client connected event
        ClientConnectedEvent clientConnectedEvent = clientConnectedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Send test datagram
        String payload = "helloworld";
        dispatcher.getDispatcher().dispatch(createPayloadDatagram(socketAddress,
                1, 2, 3, HEADER_SYS_NOVALUE, payload));
        // Get payload
        IncomingPayloadEvent incomingPayloadEvent =
                incomingPayloadEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Checks
        assertNotNull(incomingHeaderEvent);
        assertEquals(socketAddress, incomingHeaderEvent.getSocketAddress());
        assertNotNull(clientAccessRequestEvent);
        assertEquals(socketAddress, clientAccessRequestEvent.getSocketAddress());
        assertNotNull(clientConnectedEvent);
        assertEquals(socketAddress, clientConnectedEvent.getSocketAddress());
        assertEquals(clientUid, clientConnectedEvent.getClientUid());
        assertNotNull(incomingPayloadEvent);
        assertEquals(clientUid, incomingPayloadEvent.getClientUid());
        assertEquals(payload, readPayload(incomingPayloadEvent.getPayload()));
    }

    @Test
    public void testDisconnectInterval() throws InterruptedException {
        // Send accessKey first
        long accessKey = generateAccessKey();
        SocketAddress socketAddress = generateSocketAddress();
        dispatcher.getDispatcher().dispatch(createAccessRequestDatagram(socketAddress,
                1, 0, 0, HEADER_SYS_NOVALUE, accessKey));
        // InputService check disconnect interval for clients every tick
        Thread.sleep(PROPERTY_DISCONNECT_INTERVAL * 2);
        dispatcher.getDispatcher().dispatch(new TickEvent(1, 0));
        // Waiting disconnect event
        ClientDisconnectedEvent clientDisconnectedEvent =
                clientDisconnectedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got {}", clientDisconnectedEvent);
        // Assertss
        assertNotNull(clientDisconnectedEvent);
        assertEquals(socketAddress, clientDisconnectedEvent.getSocketAddress());
    }

    @Test
    public void testDisconnectRequest() throws InterruptedException {
        // Send accessKey first
        long accessKey = generateAccessKey();
        SocketAddress socketAddress = generateSocketAddress();
        dispatcher.getDispatcher().dispatch(createAccessRequestDatagram(socketAddress,
                1, 0, 0, HEADER_SYS_NOVALUE, accessKey));
        IncomingHeaderEvent incomingHeaderEvent =
                incomingHeaderEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        long clientUid = incomingHeaderEvent.getClientUid();
        // Send disconnect call for clientUid
        dispatcher.getDispatcher().dispatch(new DisconnectClientRequestEvent(clientUid));
        // Waiting for disconnection event
        ClientDisconnectedEvent clientDisconnectedEvent =
                clientDisconnectedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got {}", clientDisconnectedEvent);
        // Asserts
        assertNotNull(clientDisconnectedEvent);
        assertEquals(socketAddress, clientDisconnectedEvent.getSocketAddress());
    }

    private class ConsumerStub extends Bolt implements
            IncomingHeaderEvent.Handler,
            ClientAccessRequestEvent.Handler,
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
        public void handleClientAccessRequest(ClientAccessRequestEvent event) throws InterruptedException {
            clientAccessRequestEvents.put(event);
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
            dispatcher.getDispatcher().subscribe(this, ClientAccessRequestEvent.class);
            dispatcher.getDispatcher().subscribe(this, IncomingPayloadEvent.class);
            dispatcher.getDispatcher().subscribe(this, ClientConnectedEvent.class);
            dispatcher.getDispatcher().subscribe(this, ClientDisconnectedEvent.class);
        }
    }
}
