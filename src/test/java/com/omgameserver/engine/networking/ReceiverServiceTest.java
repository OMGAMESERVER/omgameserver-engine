package com.omgameserver.engine.networking;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.events.IncomingDatagramEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ReceiverServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(ReceiverServiceTest.class);

    private Channel serverChannel;
    private ReceiverService receiverService;
    private ConsumerStub consumerStub;

    private BlockingQueue<IncomingDatagramEvent> incomingDatagramEvents;

    @Before
    public void beforeTest() throws IOException {
        createComponents();
        serverChannel = new Channel(properties);
        receiverService = new ReceiverService(threadPoolTaskExecutor, dispatcher, serverChannel);
        receiverService.postConstruct();
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
        incomingDatagramEvents = new LinkedBlockingQueue<>(32);
    }

    @After
    public void afterTest() throws IOException {
        consumerStub.finish();
        receiverService.finish();
        serverChannel.close();
    }

    @Test
    public void testReceiver() throws IOException, InterruptedException {
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
        // Waiting event
        IncomingDatagramEvent incomingEvent = incomingDatagramEvents.poll(1000, TimeUnit.MILLISECONDS);
        ByteBuffer incomingDatagram = incomingEvent.getByteBuffer();
        byte[] bytes = new byte[incomingDatagram.remaining()];
        incomingDatagram.get(bytes);
        String incomingPayload = new String(bytes);
        SocketAddress sourceAddress = incomingEvent.getSocketAddress();
        logger.info("Server got '{}' from address {}", incomingPayload, sourceAddress);
        assertEquals(incomingPayload, outgoingPayload);
        assertEquals(sourceAddress, clientChannel.getLocalAddress());
    }

    private class ConsumerStub extends Bolt implements IncomingDatagramEvent.Handler {
        private final Logger logger = LoggerFactory.getLogger(ConsumerStub.class);

        public ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleIncomingDatagram(IncomingDatagramEvent event) throws InterruptedException {
            incomingDatagramEvents.put(event);
        }

        public void postConstruct() {
            threadPoolTaskExecutor.execute(this);
            dispatcher.subscribe(this, IncomingDatagramEvent.class);
        }
    }
}
