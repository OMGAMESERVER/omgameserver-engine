package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.events.IncomingDatagramEvent;
import com.omgameserver.engine.events.IncomingRawDataEvent;
import com.omgameserver.engine.events.SecretKeyAssignedEvent;
import com.omgameserver.engine.events.SecretKeyCreatedEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DecryptionServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(DecryptionServiceTest.class);

    private DecryptionService decryptionService;
    private BlockingQueue<SecretKeyAssignedEvent> secretKeyAssignedEvents;
    private BlockingQueue<IncomingRawDataEvent> incomingRawDataEvents;
    private ConsumerStub consumerStub;

    @Before
    public void beforeTest() throws UnknownHostException {
        createComponents();
        decryptionService = new DecryptionService(properties, executors, dispatcher);
        decryptionService.postConstruct();
        secretKeyAssignedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        incomingRawDataEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @After
    public void afterTest() {
        decryptionService.finish();
        consumerStub.finish();
    }

    @Test
    public void testKeyAssignation() throws NoSuchAlgorithmException, InterruptedException {
        long keyUid = 1;
        SecretKey secretKey = createSecretKey();
        dispatcher.getDispatcher().dispatch(new SecretKeyCreatedEvent(keyUid, secretKey));
        SocketAddress socketAddress = generateSocketAddress();
        ByteBuffer datagram = ByteBuffer.allocate(PROPERTY_DATAGRAM_SIZE);
        datagram.putLong(keyUid);
        datagram.flip();
        dispatcher.getDispatcher().dispatch(new IncomingDatagramEvent(socketAddress, datagram));
        // Waiting key assignition to socket address
        SecretKeyAssignedEvent assignedEvent = secretKeyAssignedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(assignedEvent);
        assertEquals(keyUid, assignedEvent.getKeyUid());
        assertEquals(secretKey, assignedEvent.getSecretKey());
        assertEquals(socketAddress, assignedEvent.getSocketAddress());
    }

    @Test
    public void testDecryption() throws NoSuchAlgorithmException, GeneralSecurityException, InterruptedException {
        // Generate key
        long keyUid = 1;
        SecretKey secretKey = createSecretKey();
        dispatcher.getDispatcher().dispatch(new SecretKeyCreatedEvent(keyUid, secretKey));
        // Create datagram
        SocketAddress socketAddress = generateSocketAddress();
        ByteBuffer datagram = ByteBuffer.allocate(PROPERTY_DATAGRAM_SIZE);
        datagram.putLong(keyUid);
        // Cipher data
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        String testText = "helloworld";
        byte[] cipherBytes = cipher.doFinal(testText.getBytes());
        datagram.put(cipherBytes);
        datagram.flip();
        dispatcher.getDispatcher().dispatch(new IncomingDatagramEvent(socketAddress, datagram));
        // Wait result event
        IncomingRawDataEvent rawDataEvent = incomingRawDataEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(rawDataEvent);
        // Read raw data
        ByteBuffer rawData = rawDataEvent.getRawData();
        byte[] bytes = new byte[rawData.remaining()];
        rawData.get(bytes);
        String resultText = new String(bytes);
        assertEquals(testText, resultText);
    }

    private class ConsumerStub extends Bolt implements
            SecretKeyAssignedEvent.Handler,
            IncomingRawDataEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleSecretKeyAssigned(SecretKeyAssignedEvent event) throws InterruptedException {
            secretKeyAssignedEvents.put(event);
        }

        @Override
        public void handleIncomingRawData(IncomingRawDataEvent event) throws InterruptedException {
            incomingRawDataEvents.put(event);
        }

        void postConstruct() {
            executors.executeInInternalPool(this);
            dispatcher.getDispatcher().subscribe(this, SecretKeyAssignedEvent.class);
            dispatcher.getDispatcher().subscribe(this, IncomingRawDataEvent.class);
        }
    }
}
