package com.omgameserver.engine.udp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.events.CoreTickEvent;
import com.omgameserver.engine.udp.events.UdpIncomingHeaderEvent;
import com.omgameserver.engine.udp.events.UdpOutgoingDatagramEvent;
import com.omgameserver.engine.udp.events.UdpOutgoingPayloadEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class UdpOutputServiceTest extends BaseServiceTest implements UdpHeaderConstants {
    static private final Logger logger = LoggerFactory.getLogger(UdpOutputServiceTest.class);

    private UdpOutputService outputService;
    private ConsumerStub consumerStub;

    private BlockingQueue<UdpOutgoingDatagramEvent> outgoingDatagramEvents;

    @BeforeEach
    void beforeEach() throws IOException {
        createComponents();
        outputService = new UdpOutputService(coreExecutors, coreDispatcher, udpProperties);
        outputService.postConstruct();
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
        outgoingDatagramEvents = new LinkedBlockingQueue<>(UDP_QUEUE_SIZE);
    }

    @AfterEach
    void afterEach() {
        consumerStub.finish();
        outputService.finish();
    }

    @Test
    void testClientPingRequest() throws InterruptedException {
        // Send header
        long clientUid = generateClientUid();
        SocketAddress socketAddress = generateSocketAddress();
        coreDispatcher.dispatch(new UdpIncomingHeaderEvent(socketAddress, clientUid,
                1, 0, 0, HEADER_SYS_PINGREQ));
        // Get pong response
        UdpOutgoingDatagramEvent outgoingDatagramEvent =
                outgoingDatagramEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Read header
        ByteBuffer datagram = outgoingDatagramEvent.getDatagram();
        int seq = datagram.getInt();
        int ack = datagram.getInt();
        int bit = datagram.getInt();
        int sys = datagram.get();
        // Check
        assertEquals(HEADER_SYS_PONGRES, sys);
    }

    @Test
    void testServerPingRequest() throws InterruptedException {
        // Send header
        long clientUid = generateClientUid();
        SocketAddress socketAddress = generateSocketAddress();
        coreDispatcher.dispatch(new UdpIncomingHeaderEvent(socketAddress, clientUid,
                1, 0, 0, HEADER_SYS_NOVALUE));
        // Send tick after ping interval
        Thread.sleep(udpProperties.getPingInterval() * 2);
        coreDispatcher.dispatch(new CoreTickEvent(1, 0));
        // Waiting ping request
        UdpOutgoingDatagramEvent outgoingDatagramEvent =
                outgoingDatagramEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Read header
        ByteBuffer datagram = outgoingDatagramEvent.getDatagram();
        int seq = datagram.getInt();
        int ack = datagram.getInt();
        int bit = datagram.getInt();
        int sys = datagram.get();
        // Checks
        assertEquals(socketAddress, outgoingDatagramEvent.getTargetAddress());
        assertEquals(HEADER_SYS_PINGREQ, sys);
    }

    @Test
    void testExchange() throws InterruptedException {
        for (int clientUid = 1; clientUid <= 10; clientUid++) {
            logger.info("clientUid={}", clientUid);
            SocketAddress socketAddress = new InetSocketAddress("0.0.0.0", 10000 + clientUid);
            logger.info("socketAddress={}", socketAddress);
            // Initialize connection
            coreDispatcher.dispatch(
                    new UdpIncomingHeaderEvent(socketAddress, clientUid, 1, 0, 0, HEADER_SYS_NOVALUE));
            double lossSimulationLevel = 0.1 + Math.random() * 0.4;
            logger.info("lossSimulationLevel={}", lossSimulationLevel);
            int lastIncomingBit = 0;
            int lastIncomingSeq = 0;
            int lastOutgoingSeq = 1;
            Set waiting = new HashSet();
            // Exchange
            int lastNumber = 128;
            int iteration = 1;
            while (iteration <= lastNumber || waiting.size() > 0) {
                // Send from server to client
                if (iteration <= lastNumber) {
                    logger.info("Send number {}", iteration);
                    ByteBuffer outgoingByteBuffer = ByteBuffer.allocate(Integer.BYTES);
                    outgoingByteBuffer.putInt(iteration);
                    outgoingByteBuffer.flip();
                    coreDispatcher.dispatch(new UdpOutgoingPayloadEvent(clientUid, outgoingByteBuffer, true));
                    // Save data to waiting on client
                    waiting.add(iteration);
                }
                // Flush on server works every tick
                coreDispatcher.dispatch(new CoreTickEvent(iteration, 0));
                iteration++;
                // Handle server datagrams
                UdpOutgoingDatagramEvent event =
                        outgoingDatagramEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (event != null) {
                    int incomingSeq = event.getDatagram().getInt();
                    int incomingAck = event.getDatagram().getInt();
                    int incomingBit = event.getDatagram().getInt();
                    int incomingSys = event.getDatagram().get();
                    if (incomingSys == HEADER_SYS_PINGREQ) {
                        // Apply header
                        lastIncomingBit = (lastIncomingBit << incomingSeq - lastIncomingSeq) | 1;
                        lastIncomingSeq = incomingSeq;
                        continue;
                    }
                    // Simulate data lost
                    if (iteration <= lastNumber && Math.random() < lossSimulationLevel) {
                        continue;
                    }
                    // Apply header
                    lastIncomingBit = (lastIncomingBit << incomingSeq - lastIncomingSeq) | 1;
                    lastIncomingSeq = incomingSeq;
                    Set payload = new HashSet();
                    // Remove data from waiting list
                    ByteBuffer incomingByteBuffer = event.getDatagram();
                    while (event.getDatagram().hasRemaining()) {
                        int number = incomingByteBuffer.getInt();
                        payload.add(number);
                        waiting.remove(number);
                    }
                    logger.info("Incoming seq={}, ack={}, bit={}, sys={}, payload={} waiting={}",
                            incomingSeq, incomingAck, Integer.toBinaryString(incomingBit), incomingSys, payload, waiting);
                    // Notify server about incoming and missing datagrams
                    lastOutgoingSeq++;
                    logger.info("Outgoing seq={}, ack={}, bit={}", lastOutgoingSeq, lastIncomingSeq, Integer.toBinaryString(lastIncomingBit));
                    coreDispatcher.dispatch(new UdpIncomingHeaderEvent(socketAddress, clientUid, lastOutgoingSeq, lastIncomingSeq,
                            lastIncomingBit, HEADER_SYS_NOVALUE));
                }
            }
            assertTrue(waiting.size() == 0);
        }
    }

    private class ConsumerStub extends Bolt implements
            UdpOutgoingDatagramEvent.Handler {

        public ConsumerStub() {
            super("consumer-stub", UDP_QUEUE_SIZE);
        }

        @Override
        public void handleUdpOutgoingDatagram(UdpOutgoingDatagramEvent event) throws InterruptedException {
            outgoingDatagramEvents.put(event);
        }

        public void postConstruct() {
            coreExecutors.executeInInternalPool(this);
            coreDispatcher.getDispatcher().subscribe(this, UdpOutgoingDatagramEvent.class);
        }
    }
}
