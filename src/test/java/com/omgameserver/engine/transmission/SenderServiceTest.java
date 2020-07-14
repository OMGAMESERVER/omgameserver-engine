package com.omgameserver.engine.transmission;

import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.events.OutgoingDatagramEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public class SenderServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(SenderServiceTest.class);

    private SenderService senderService;
    private Channel serverChannel;

    @Before
    public void beforeTest() throws IOException {
        createComponents();
        serverChannel = new Channel(properties);
        senderService = new SenderService(properties, executors, dispatcher, serverChannel);
        senderService.postConstruct();
    }

    @After
    public void afterTest() throws IOException {
        senderService.finish();
        serverChannel.close();
    }

    @Test
    public void testSender() throws InterruptedException, IOException {
        // Connect to server
        DatagramChannel clientChannel = DatagramChannel.open();
        clientChannel.connect(serverChannel.getAddress());
        logger.info("Client connected to {}", serverChannel.getAddress());
        // Send from server to test client
        String outgoingPayload = "helloworld";
        ByteBuffer outgoingDatagram = ByteBuffer.allocate(outgoingPayload.length());
        outgoingDatagram.put(outgoingPayload.getBytes());
        outgoingDatagram.flip();
        SocketAddress targetAddress = clientChannel.getLocalAddress();
        logger.info("Server send '{}' to {}", outgoingPayload, targetAddress);
        dispatcher.dispatch(new OutgoingDatagramEvent(targetAddress, outgoingDatagram));
        // Receive on test client
        ByteBuffer incomingDatagram = ByteBuffer.allocate(1024);
        clientChannel.receive(incomingDatagram);
        incomingDatagram.flip();
        byte[] bytes = new byte[incomingDatagram.remaining()];
        incomingDatagram.get(bytes);
        String incomingPayload = new String(bytes);
        logger.info("Client got '{}'", outgoingPayload);
        // Check
        assertEquals(outgoingPayload, incomingPayload);
    }
}
