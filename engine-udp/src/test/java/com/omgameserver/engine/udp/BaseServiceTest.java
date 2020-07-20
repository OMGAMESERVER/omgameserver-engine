package com.omgameserver.engine.udp;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.core.CoreProperties;
import com.omgameserver.engine.core.CoreUidGenerator;
import com.omgameserver.engine.udp.events.UdpIncomingDatagramEvent;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class BaseServiceTest extends Assertions {
    static private final Logger logger = LoggerFactory.getLogger(BaseServiceTest.class);

    protected final int POLL_TIMEOUT_MS = 1000;

    protected final int CORE_INTERNAL_THREAD_POOL_SIZE = 16;
    protected final int CORE_USER_THREAD_POOL_SIZE = 32;
    protected final int CORE_TICK_INTERVAL = 100;

    protected final int UDP_QUEUE_SIZE = 128;
    protected final String UDP_HOST = "0.0.0.0";
    protected final int UDP_PORT = 0;
    protected final int UDP_DATAGRAM_SIZE = 1024;
    protected final int UDP_DISCONNECT_INTERVAL = 1000;
    protected final int UDP_PING_INTERVAL = 250;

    protected CoreProperties coreProperties;
    protected CoreDispatcher coreDispatcher;
    protected CoreUidGenerator coreUidGenerator;
    protected CoreExecutors coreExecutors;
    protected UdpProperties udpProperties;

    protected void createComponents() throws UnknownHostException {
        coreProperties = new CoreProperties(CORE_INTERNAL_THREAD_POOL_SIZE, CORE_USER_THREAD_POOL_SIZE,
                CORE_TICK_INTERVAL);
        coreDispatcher = new CoreDispatcher();
        coreUidGenerator = new CoreUidGenerator();
        coreExecutors = new CoreExecutors(coreProperties);
        udpProperties = new UdpProperties(UDP_QUEUE_SIZE, UDP_HOST, UDP_PORT, UDP_DATAGRAM_SIZE,
                UDP_DISCONNECT_INTERVAL, UDP_PING_INTERVAL, 0);
    }

    protected SocketAddress generateSocketAddress() {
        SocketAddress socketAddress = new InetSocketAddress("0.0.0.0", (int) (Math.random() * 55535) + 10000);
        logger.info("New socketAddress={} generated", socketAddress);
        return socketAddress;
    }

    protected Long generateClientUid() {
        Long clientUid = Long.valueOf((int) (Math.random() * 89999 + 10000));
        logger.info("New clientUid={} generated", clientUid);
        return clientUid;
    }

    protected UdpIncomingDatagramEvent createIncomingDatagramEvent(SocketAddress sourceAddress, int seq, int ack,
                                                                   int bit, byte sys, String payload) {
        ByteBuffer rawData = ByteBuffer.allocate(udpProperties.getDatagramSize());
        rawData.putInt(seq);
        rawData.putInt(ack);
        rawData.putInt(bit);
        rawData.put(sys);
        rawData.put(payload.getBytes());
        rawData.flip();
        return new UdpIncomingDatagramEvent(sourceAddress, rawData);
    }

    protected String readPayload(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return new String(bytes);
    }
}
