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

public class InputServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(InputServiceTest.class);

    private InputService inputService;
    private ConsumerStub consumerStub;

    private BlockingQueue<IncomingHeaderEvent> incomingHeaderEvents;
    private BlockingQueue<IncomingPayloadEvent> incomingPayloadEvents;
    private BlockingQueue<ClientDisconnectedEvent> clientDisconnectedEvents;

    @Before
    public void beforeTest() throws IOException {
        createComponents();
        inputService = new InputService(properties, threadPoolTaskExecutor, dispatcher);
        inputService.postConstruct();
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
        incomingHeaderEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        incomingPayloadEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        clientDisconnectedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
    }

    @After
    public void afterTest() {
        consumerStub.finish();
        inputService.finish();
    }

    @Test
    public void testSplitHeaderAndPayload() throws InterruptedException {
        // Send datagram with specified source address and header
        SocketAddress sourceAddress = generateSocketAddress();
        dispatcher.dispatch(createIncomingRawDataEvent(sourceAddress,
                1, 2,3, (byte) 0,"payload"));
        // Waiting header event
        IncomingHeaderEvent incomingHeaderEvent = incomingHeaderEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got {}", incomingHeaderEvent);
        // Checks
        assertNotNull(incomingHeaderEvent);
        assertTrue(incomingHeaderEvent.getSocketAddress() == sourceAddress);
        assertTrue(incomingHeaderEvent.getSeq() == 1);
        assertTrue(incomingHeaderEvent.getAck() == 2);
        assertTrue(incomingHeaderEvent.getBit() == 3);
        assertTrue(incomingHeaderEvent.getSys() == 0);
    }

    @Test
    public void testPayloadSplit() throws InterruptedException {
        // Send datagram with specified payload
        String testPayload = "payload";
        dispatcher.dispatch(createIncomingRawDataEvent(generateSocketAddress(),
                1, 2,3, (byte) 0, testPayload));
        // Waiting for payload event
        IncomingPayloadEvent incomingPayloadEvent = incomingPayloadEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got {}", incomingPayloadEvent);
        // Asserts
        assertNotNull(incomingPayloadEvent);
        String incomingPayload = readPayload(incomingPayloadEvent.getPayload());
        assertEquals(testPayload, incomingPayload);
    }

    @Test
    public void testDisconnectInterval() throws InterruptedException {
        // Send datagram to client creation
        SocketAddress sourceAddress = generateSocketAddress();
        dispatcher.dispatch(createIncomingRawDataEvent(sourceAddress,
                1, 0 ,0, (byte) 0, "payload"));
        // InputService check disconnect interval for clients every tick
        Thread.sleep(properties.getDisconnectInterval() * 2);
        dispatcher.dispatch(new TickEvent(1,PROPERTY_DISCONNECT_INTERVAL * 2));
        // Waiting for disconnect event
        ClientDisconnectedEvent clientDisconnectedEvent =
                clientDisconnectedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got {}", clientDisconnectedEvent);
        // Asserts
        assertNotNull(clientDisconnectedEvent);
        assertTrue(clientDisconnectedEvent.getSocketAddress() == sourceAddress);
    }

    @Test
    public void testDisconnectRequest() throws InterruptedException {
        // Send datagram to client creation
        SocketAddress sourceAddress = generateSocketAddress();
        dispatcher.dispatch(createIncomingRawDataEvent(sourceAddress,
                1, 0 ,0, (byte) 0, "payload"));
        // Waiting for payload event with clientUid
        IncomingPayloadEvent payloadEvent = incomingPayloadEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got {}", payloadEvent);
        assertNotNull(payloadEvent);
        // Send disconnect call for clientUid
        Long clientUid = payloadEvent.getClientUid();
        logger.info("Send disconnect call for clientUid={}", clientUid);
        dispatcher.dispatch(new DisconnectClientRequestEvent(clientUid));
        // Waiting for disconnection event
        ClientDisconnectedEvent clientDisconnectedEvent =
                clientDisconnectedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got {}", clientDisconnectedEvent);
        // Asserts
        assertNotNull(clientDisconnectedEvent);
        assertTrue(clientDisconnectedEvent.getSocketAddress() == sourceAddress);
    }

    private class ConsumerStub extends Bolt implements
            IncomingHeaderEvent.Handler,
            IncomingPayloadEvent.Handler,
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
        public void handleClientDisconnected(ClientDisconnectedEvent event) throws InterruptedException {
            clientDisconnectedEvents.put(event);
        }

        public void postConstruct() {
            threadPoolTaskExecutor.execute(this);
            dispatcher.subscribe(this, IncomingHeaderEvent.class);
            dispatcher.subscribe(this, IncomingPayloadEvent.class);
            dispatcher.subscribe(this, ClientDisconnectedEvent.class);
        }
    }
}