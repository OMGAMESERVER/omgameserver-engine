package com.omgameserver.engine.udp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.udp.events.UdpIncomingDatagramEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class UdpReceiverServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(UdpReceiverServiceTest.class);

    private UdpChannel serverChannel;
    private UdpReceiverService receiverService;
    private ConsumerStub consumerStub;

    private BlockingQueue<UdpIncomingDatagramEvent> incomingDatagramEvents;

    @BeforeEach
    void beforeEach() throws IOException {
        createComponents();
        serverChannel = new UdpChannel(udpProperties);
        receiverService = new UdpReceiverService(coreExecutors, coreDispatcher, serverChannel);
        receiverService.postConstruct();
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
        incomingDatagramEvents = new LinkedBlockingQueue<>(32);
    }

    @AfterEach
    void afterEach() throws IOException {
        consumerStub.finish();
        receiverService.finish();
        serverChannel.close();
    }

    @Test
    void testReceiver() throws IOException, InterruptedException {
        // Connect to server
        DatagramChannel clientChannel = DatagramChannel.open();
        clientChannel.connect(serverChannel.getAddress());
        logger.info("Client connected to {}", serverChannel.getAddress());
        // Create datagram
        String outgoingPayload = "helloworld";
        ByteBuffer outgoingDatagram = ByteBuffer.allocate(outgoingPayload.length());
        outgoingDatagram.put(outgoingPayload.getBytes());
        outgoingDatagram.flip();
        clientChannel.write(outgoingDatagram);
        // Waiting events
        UdpIncomingDatagramEvent incomingEvent = incomingDatagramEvents.poll(1000, TimeUnit.MILLISECONDS);
        ByteBuffer incomingDatagram = incomingEvent.getByteBuffer();
        byte[] bytes = new byte[incomingDatagram.remaining()];
        incomingDatagram.get(bytes);
        String incomingPayload = new String(bytes);
        SocketAddress sourceAddress = incomingEvent.getSocketAddress();
        logger.info("Server got '{}' from address {}", incomingPayload, sourceAddress);
        assertEquals(incomingPayload, outgoingPayload);
        assertEquals(sourceAddress, clientChannel.getLocalAddress());
    }

    private class ConsumerStub extends Bolt implements UdpIncomingDatagramEvent.Handler {

        public ConsumerStub() {
            super("consumer-stub", UDP_QUEUE_SIZE);
        }

        @Override
        public void handleUdpIncomingDatagram(UdpIncomingDatagramEvent event) throws InterruptedException {
            incomingDatagramEvents.put(event);
        }

        public void postConstruct() {
            coreExecutors.executeInInternalPool(this);
            coreDispatcher.getDispatcher().subscribe(this, UdpIncomingDatagramEvent.class);
        }
    }
}
