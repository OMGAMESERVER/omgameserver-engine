package com.omgameserver.engine;

import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.events.IncomingRawDataEvent;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class BaseServiceTest extends Assert implements OmgsConstants {
    static private final Logger logger = LoggerFactory.getLogger(BaseServiceTest.class);

    protected final int POLL_TIMEOUT_MS = 1000;

    protected final String PROPERTY_HOST = "0.0.0.0";
    protected final int PROPERTY_PORT = 0;
    protected final int PROPERTY_THREAD_POOL_SIZE = 32;
    protected final int PROPERTY_QUEUE_SIZE = 128;
    protected final int PROPERTY_DATAGRAM_SIZE = 508;
    protected final int PROPERTY_SECRET_KEY_LIFETIME = 1000;
    protected final int PROPERTY_TICK_INTERVAL = 100;
    protected final int PROPERTY_DISCONNECT_INTERVAL = 1000;
    protected final int PROPERTY_PING_INTERVAL = 250;
    protected final String PROPERTY_MAIN_SCRIPT = "main.lua";

    protected OmgsProperties properties;
    protected OmgsDispatcher dispatcher;
    protected OmgsExecutors executors;

    protected void createComponents() throws UnknownHostException {
        properties = new OmgsProperties(PROPERTY_HOST, PROPERTY_PORT, PROPERTY_THREAD_POOL_SIZE, PROPERTY_QUEUE_SIZE,
                PROPERTY_DATAGRAM_SIZE, PROPERTY_SECRET_KEY_LIFETIME, PROPERTY_TICK_INTERVAL,
                PROPERTY_DISCONNECT_INTERVAL, PROPERTY_PING_INTERVAL, PROPERTY_MAIN_SCRIPT);
        dispatcher = new OmgsDispatcher();
        executors = new OmgsExecutors(properties);
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

    protected IncomingRawDataEvent createIncomingRawDataEvent(SocketAddress sourceAddress, int seq, int ack, int bit,
                                                              byte sys, String payload) {
        ByteBuffer rawData = ByteBuffer.allocate(properties.getDatagramSize());
        rawData.putInt(seq);
        rawData.putInt(ack);
        rawData.putInt(bit);
        rawData.put(sys);
        rawData.put(payload.getBytes());
        rawData.flip();
        return new IncomingRawDataEvent(sourceAddress, rawData);
    }

    protected SecretKey createSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = new SecureRandom();
        keyGenerator.init(128, secureRandom);
        return keyGenerator.generateKey();
    }

    protected ByteBuffer skipHeader(ByteBuffer byteBuffer) {
        byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.get();
        return byteBuffer;
    }

    protected String readPayload(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return new String(bytes);
    }
}
