package com.omgameserver.engine.networking;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.OmgsConstants;
import com.omgameserver.engine.events.IncomingHeaderEvent;
import com.omgameserver.engine.events.OutgoingDatagramEvent;
import com.omgameserver.engine.events.OutgoingPayloadEvent;
import com.omgameserver.engine.events.TickEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

public class OutputServiceTest extends BaseServiceTest implements OmgsConstants {
    static private final Logger logger = LoggerFactory.getLogger(OutputServiceTest.class);

    private OutputService outputService;
    private ConsumerStub consumerStub;

    private BlockingQueue<OutgoingDatagramEvent> outgoingDatagramEvents;

    @Before
    public void beforeTest() throws IOException {
        createComponents();
        outputService = new OutputService(properties, threadPoolTaskExecutor, dispatcher);
        outputService.postConstruct();
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
        outgoingDatagramEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
    }

    @After
    public void afterTest() {
        consumerStub.finish();
        outputService.finish();
    }

    @Test
    public void testPong() throws InterruptedException {
        // Send ping request
        SocketAddress socketAddress = generateSocketAddress();
        IncomingHeaderEvent headerEvent = new IncomingHeaderEvent(socketAddress, generateClientUid(),
                1, 0, 0, HEADER_SYS_PINGREQ);
        dispatcher.dispatch(headerEvent);
        // Waiting pong response
        OutgoingDatagramEvent pongEvent =
                outgoingDatagramEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Read header
        ByteBuffer byteBuffer = pongEvent.getByteBuffer();
        int seq = byteBuffer.getInt();
        int ack = byteBuffer.getInt();
        int bit = byteBuffer.getInt();
        int sys = byteBuffer.get();
        logger.info("Got datagram with seq={}, ack={}, bit={}, sys={}", seq, ack, Integer.toBinaryString(bit), sys);
        // Asserts
        assertEquals(socketAddress, pongEvent.getTargetAddress());
        assertTrue((sys & HEADER_SYS_PONGRES) > 0);
    }

    @Test
    public void testPing() throws InterruptedException {
        // Send header event for client creation
        SocketAddress socketAddress = generateSocketAddress();
        IncomingHeaderEvent headerEvent = new IncomingHeaderEvent(socketAddress, generateClientUid(),
                1, 0, 0, HEADER_SYS_NOVALUE);
        dispatcher.dispatch(headerEvent);
        // Output client check ping interval every tick event
        Thread.sleep(properties.getPingInterval() * 2);
        TickEvent tickEvent = new TickEvent(1, 0);
        dispatcher.dispatch(tickEvent);
        // Waiting ping request
        OutgoingDatagramEvent outgoingDatagramEvent =
                outgoingDatagramEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Read header
        ByteBuffer byteBuffer = outgoingDatagramEvent.getByteBuffer();
        int seq = byteBuffer.getInt();
        int ack = byteBuffer.getInt();
        int bit = byteBuffer.getInt();
        int sys = byteBuffer.get();
        logger.info("Got datagram with seq={}, ack={}, bit={}, sys={}", seq, ack, Integer.toBinaryString(bit), sys);
        assertEquals(socketAddress, outgoingDatagramEvent.getTargetAddress());
        assertTrue((sys & HEADER_SYS_PINGREQ) > 0);
    }

    @Test
    public void testExchange() throws InterruptedException {
        for (int clientUid = 1; clientUid <= 10; clientUid++) {
            logger.info("clientUid={}", clientUid);
            SocketAddress socketAddress = new InetSocketAddress("0.0.0.0", 10000 + clientUid);
            logger.info("socketAddress={}", socketAddress);
            // Initialize connection
            dispatcher.dispatch(
                    new IncomingHeaderEvent(socketAddress, clientUid,1,0,0, HEADER_SYS_NOVALUE));
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
                    dispatcher.dispatch(new OutgoingPayloadEvent(clientUid, outgoingByteBuffer, false));
                    // Save data to waiting on client
                    waiting.add(iteration);
                }
                // Flush on server works every tick
                dispatcher.dispatch(new TickEvent(iteration,0));
                iteration++;
                // Handle server datagrams
                OutgoingDatagramEvent event =
                        outgoingDatagramEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (event != null) {
                    int incomingSeq = event.getByteBuffer().getInt();
                    int incomingAck = event.getByteBuffer().getInt();
                    int incomingBit = event.getByteBuffer().getInt();
                    int incomingSys = event.getByteBuffer().get();
                    if ((incomingSys & HEADER_SYS_PINGREQ) > 0) {
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
                    ByteBuffer incomingByteBuffer = event.getByteBuffer();
                    while (event.getByteBuffer().hasRemaining()) {
                        int number = incomingByteBuffer.getInt();
                        payload.add(number);
                        waiting.remove(number);
                    }
                    logger.info("Incoming seq={}, ack={}, bit={}, sys={}, payload={} waiting={}",
                            incomingSeq, incomingAck, Integer.toBinaryString(incomingBit), incomingSys, payload, waiting);
                    // Notify server about incoming and missing datagrams
                    lastOutgoingSeq++;
                    logger.info("Outgoing seq={}, ack={}, bit={}", lastOutgoingSeq, lastIncomingSeq, Integer.toBinaryString(lastIncomingBit));
                    dispatcher.dispatch(new IncomingHeaderEvent(socketAddress, clientUid, lastOutgoingSeq, lastIncomingSeq,
                            lastIncomingBit, HEADER_SYS_NOVALUE));
                }
            }
            assertTrue(waiting.size() == 0);
        }
    }

    private class ConsumerStub extends Bolt implements
            OutgoingDatagramEvent.Handler {

        public ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleOutgoingDatagram(OutgoingDatagramEvent event) throws InterruptedException {
            outgoingDatagramEvents.put(event);
        }

        public void postConstruct() {
            threadPoolTaskExecutor.execute(this);
            dispatcher.subscribe(this, OutgoingDatagramEvent.class);
        }
    }
}
