package com.omgameserver.engine.udp;

import com.omgameserver.engine.udp.events.UdpOutgoingDatagramEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class UdpSenderServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(UdpSenderServiceTest.class);

    private UdpSenderService senderService;
    private UdpChannel serverChannel;

    @BeforeEach
    void beforeEach() throws IOException {
        createComponents();
        serverChannel = new UdpChannel(udpProperties);
        senderService = new UdpSenderService(coreExecutors, coreDispatcher, udpProperties, serverChannel);
        senderService.postConstruct();
    }

    @AfterEach
    public void afterEach() throws IOException {
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
        coreDispatcher.dispatch(new UdpOutgoingDatagramEvent(targetAddress, outgoingDatagram));
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
