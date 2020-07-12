package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.events.OutgoingDatagramEvent;
import com.omgameserver.engine.events.OutgoingRawDataEvent;
import com.omgameserver.engine.events.SecretKeyAssignedEvent;
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

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public class EncryptionServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(EncryptionServiceTest.class);

    private EncryptionService encryptionService;
    private BlockingQueue<OutgoingDatagramEvent> outgoingDatagramEvents;
    private ConsumerStub consumerStub;

    @Before
    public void beforeTest() throws UnknownHostException {
        createComponents();
        encryptionService = new EncryptionService(properties, executors, dispatcher);
        encryptionService.postConstruct();
        outgoingDatagramEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @After
    public void afterTest() {
        encryptionService.finish();
        consumerStub.finish();
    }

    @Test
    public void testEncryption() throws NoSuchAlgorithmException, GeneralSecurityException, InterruptedException {
        // Generate key
        long keyUid = 1;
        SecretKey secretKey = createSecretKey();
        SocketAddress socketAddress = generateSocketAddress();
        dispatcher.getDispatcher().dispatch(new SecretKeyAssignedEvent(keyUid, secretKey, socketAddress));
        // Create rawData
        ByteBuffer rawData = ByteBuffer.allocate(PROPERTY_DATAGRAM_SIZE);
        String testData = "helloworld";
        rawData.put(testData.getBytes());
        rawData.flip();
        dispatcher.getDispatcher().dispatch(new OutgoingRawDataEvent(socketAddress, rawData));
        // Waiting result event
        OutgoingDatagramEvent datagramEvent = outgoingDatagramEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(datagramEvent);
        // Decrypt
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        ByteBuffer decryptedByteBuffer = ByteBuffer.allocate(PROPERTY_DATAGRAM_SIZE);
        cipher.doFinal(datagramEvent.getByteBuffer(), decryptedByteBuffer);
        decryptedByteBuffer.flip();
        byte[] bytes = new byte[decryptedByteBuffer.remaining()];
        decryptedByteBuffer.get(bytes);
        String resultText = new String(bytes);
        assertEquals(testData, resultText);
    }

    private class ConsumerStub extends Bolt implements
            OutgoingDatagramEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleOutgoingDatagram(OutgoingDatagramEvent event) throws InterruptedException {
            outgoingDatagramEvents.put(event);
        }

        void postConstruct() {
            executors.executeInInternalPool(this);
            dispatcher.getDispatcher().subscribe(this, OutgoingDatagramEvent.class);
        }
    }
}
