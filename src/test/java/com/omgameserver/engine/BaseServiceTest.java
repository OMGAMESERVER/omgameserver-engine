package com.omgameserver.engine;

import com.omgameserver.engine.events.IncomingDatagramEvent;
import org.junit.Assert;
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
public class BaseServiceTest extends Assert {
    static private final Logger logger = LoggerFactory.getLogger(BaseServiceTest.class);

    protected final int POLL_TIMEOUT_MS = 1000;

    protected final String PROPERTY_HOST = "0.0.0.0";
    protected final int PROPERTY_PORT = 0;
    protected final int PROPERTY_THREAD_POOL_SIZE = 32;
    protected final int PROPERTY_QUEUE_SIZE = 128;
    protected final int PROPERTY_DATAGRAM_SIZE = 508;
    protected final int PROPERTY_ACCESS_KEY_LIFETIME = 1000;
    protected final int PROPERTY_TICK_INTERVAL = 100;
    protected final int PROPERTY_DISCONNECT_INTERVAL = 1000;
    protected final int PROPERTY_PING_INTERVAL = 250;
    protected final String PROPERTY_MAIN_SCRIPT = "main.lua";

    protected EngineProperties properties;
    protected EngineDispatcher dispatcher;
    protected EngineExecutors executors;

    protected void createComponents() throws UnknownHostException {
        properties = new EngineProperties(PROPERTY_HOST, PROPERTY_PORT, PROPERTY_THREAD_POOL_SIZE, PROPERTY_QUEUE_SIZE,
                PROPERTY_DATAGRAM_SIZE, PROPERTY_ACCESS_KEY_LIFETIME, PROPERTY_TICK_INTERVAL,
                PROPERTY_DISCONNECT_INTERVAL, PROPERTY_PING_INTERVAL, PROPERTY_MAIN_SCRIPT);
        dispatcher = new EngineDispatcher();
        executors = new EngineExecutors(properties);
        logger.info("Thread pool with size={} created", properties.getThreadPoolSize());
    }

    protected SocketAddress generateSocketAddress() {
        SocketAddress socketAddress = new InetSocketAddress("0.0.0.0", (int) (Math.random() * 55535) + 10000);
        logger.info("Generated socketAddress={}", socketAddress);
        return socketAddress;
    }

    protected Long generateClientUid() {
        Long clientUid = Long.valueOf((int) (Math.random() * 89999 + 10000));
        logger.info("Generated clientUid={}", clientUid);
        return clientUid;
    }

    protected IncomingDatagramEvent createIncomingDatagramEvent(SocketAddress sourceAddress, int seq, int ack, int bit,
                                                                byte sys, String payload) {
        ByteBuffer rawData = ByteBuffer.allocate(properties.getDatagramSize());
        rawData.putInt(seq);
        rawData.putInt(ack);
        rawData.putInt(bit);
        rawData.put(sys);
        rawData.put(payload.getBytes());
        rawData.flip();
        return new IncomingDatagramEvent(sourceAddress, rawData);
    }

    protected String readPayload(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return new String(bytes);
    }
}
