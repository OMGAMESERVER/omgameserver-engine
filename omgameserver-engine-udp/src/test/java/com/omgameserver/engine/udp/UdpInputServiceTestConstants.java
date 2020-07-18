package com.omgameserver.engine.udp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.events.CoreTickEvent;
import com.omgameserver.engine.udp.events.UdpClientConnectedEvent;
import com.omgameserver.engine.udp.events.UdpClientDisconnectedEvent;
import com.omgameserver.engine.udp.events.UdpIncomingHeaderEvent;
import com.omgameserver.engine.udp.events.UdpIncomingPayloadEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class UdpInputServiceTestConstants extends BaseServiceTest implements UdpHeaderConstants {
    static private final Logger logger = LoggerFactory.getLogger(UdpInputServiceTestConstants.class);

    private UdpInputService inputService;
    private ConsumerStub consumerStub;

    private BlockingQueue<UdpIncomingHeaderEvent> incomingHeaderEvents;
    private BlockingQueue<UdpIncomingPayloadEvent> incomingPayloadEvents;
    private BlockingQueue<UdpClientConnectedEvent> clientConnectedEvents;
    private BlockingQueue<UdpClientDisconnectedEvent> clientDisconnectedEvents;

    @BeforeEach
    void beforeEach() throws IOException {
        createComponents();
        inputService = new UdpInputService(coreExecutors, coreDispatcher, coreUidGenerator, udpProperties);
        inputService.postConstruct();
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
        incomingHeaderEvents = new LinkedBlockingQueue<>(UDP_QUEUE_SIZE);
        incomingPayloadEvents = new LinkedBlockingQueue<>(UDP_QUEUE_SIZE);
        clientConnectedEvents = new LinkedBlockingQueue<>(UDP_QUEUE_SIZE);
        clientDisconnectedEvents = new LinkedBlockingQueue<>(UDP_QUEUE_SIZE);
    }

    @AfterEach
    void afterEach() {
        consumerStub.finish();
        inputService.finish();
    }

    @Test
    void testInput() throws InterruptedException {
        // Send datagram
        SocketAddress socketAddress = generateSocketAddress();
        String payload = "payload";
        coreDispatcher.dispatch(createIncomingDatagramEvent(socketAddress,
                1, 2, 3, HEADER_SYS_NOVALUE, payload));
        // Get header events
        UdpIncomingHeaderEvent incomingHeaderEvent =
                incomingHeaderEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        long clientUid = incomingHeaderEvent.getClientUid();
        // Get client connected events
        UdpClientConnectedEvent clientConnectedEvent = clientConnectedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Get payload events
        UdpIncomingPayloadEvent incomingPayloadEvent =
                incomingPayloadEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Send tick after disconnect interval
        Thread.sleep(UDP_DISCONNECT_INTERVAL * 2);
        coreDispatcher.dispatch(new CoreTickEvent(1, 0));
        // Get disconnect events
        UdpClientDisconnectedEvent clientDisconnectedEvent =
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

    private class ConsumerStub extends Bolt implements
            UdpIncomingHeaderEvent.Handler,
            UdpIncomingPayloadEvent.Handler,
            UdpClientConnectedEvent.Handler,
            UdpClientDisconnectedEvent.Handler {

        public ConsumerStub() {
            super("consumer-stub", UDP_QUEUE_SIZE);
        }

        @Override
        public void handleUdpIncomingHeader(UdpIncomingHeaderEvent event) throws InterruptedException {
            incomingHeaderEvents.put(event);
        }

        @Override
        public void handleUdpIncomingPayload(UdpIncomingPayloadEvent event) throws InterruptedException {
            incomingPayloadEvents.put(event);
        }

        @Override
        public void handleUdpClientConnected(UdpClientConnectedEvent event) throws InterruptedException {
            clientConnectedEvents.put(event);
        }

        @Override
        public void handleUdpClientDisconnected(UdpClientDisconnectedEvent event) throws InterruptedException {
            clientDisconnectedEvents.put(event);
        }

        public void postConstruct() {
            coreExecutors.executeInInternalPool(this);
            coreDispatcher.getDispatcher().subscribe(this, UdpIncomingHeaderEvent.class);
            coreDispatcher.getDispatcher().subscribe(this, UdpIncomingPayloadEvent.class);
            coreDispatcher.getDispatcher().subscribe(this, UdpClientConnectedEvent.class);
            coreDispatcher.getDispatcher().subscribe(this, UdpClientDisconnectedEvent.class);
        }
    }
}
