package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.OmgsExecutors;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class DecryptionService extends Bolt implements
        SecretKeyCreatedEvent.Handler,
        IncomingDatagramEvent.Handler,
        SecretKeyExpiredEvent.Handler,
        ClientDisconnectedEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(DecryptionService.class);

    private final OmgsExecutors executors;
    private final OmgsDispatcher dispatcher;
    private final Map<Long, SecretKey> temporaryKeys;
    private final Map<SocketAddress, Cipher> ciphers;

    DecryptionService(OmgsProperties properties, OmgsExecutors executors, OmgsDispatcher dispatcher) {
        super("decryptor", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        temporaryKeys = new HashMap<>();
        ciphers = new HashMap<>();
    }

    @Override
    public void handleSecretKeyCreated(SecretKeyCreatedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long keyUid = event.getKeyUid();
        SecretKey secretKey = event.getSecretKey();
        temporaryKeys.put(keyUid, secretKey);
    }

    @Override
    public void handleIncomingDatagram(IncomingDatagramEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        ByteBuffer byteBuffer = event.getByteBuffer();
        long keyUid = byteBuffer.getLong();
        Cipher cipher = getCipher(keyUid, socketAddress);
        if (cipher != null) {
            int outputSize = cipher.getOutputSize(byteBuffer.remaining());
            ByteBuffer rawData = ByteBuffer.allocate(outputSize);
            try {
                cipher.doFinal(byteBuffer, rawData);
                rawData.flip();
                dispatcher.getDispatcher().dispatch(new IncomingRawDataEvent(socketAddress, rawData));
            } catch (GeneralSecurityException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Encryption failed for {} as {}", socketAddress, e);
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Cipher not found for {}", socketAddress);
            }
        }
    }

    @Override
    public void handleSecretKeyExpired(SecretKeyExpiredEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        temporaryKeys.remove(event.getKeyUid());
    }

    @Override
    public void handleClientDisconnected(ClientDisconnectedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        ciphers.remove(event.getSocketAddress());
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, SecretKeyCreatedEvent.class);
        dispatcher.getDispatcher().subscribe(this, IncomingDatagramEvent.class);
        dispatcher.getDispatcher().subscribe(this, SecretKeyExpiredEvent.class);
        dispatcher.getDispatcher().subscribe(this, ClientDisconnectedEvent.class);
    }

    private Cipher getCipher(long keyUid, SocketAddress socketAddress) throws InterruptedException {
        Cipher cipher = ciphers.get(socketAddress);
        if (cipher == null) {
            SecretKey secretKey = temporaryKeys.remove(keyUid);
            if (secretKey != null) {
                try {
                    cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.DECRYPT_MODE, secretKey);
                    ciphers.put(socketAddress, cipher);
                    dispatcher.getDispatcher().dispatch(new SecretKeyAssignedEvent(keyUid, secretKey, socketAddress));
                    if (logger.isDebugEnabled()) {
                        logger.debug("Key with uid={} assigned to {}", keyUid, socketAddress);
                    }
                } catch (GeneralSecurityException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(e.getMessage(), e);
                    }
                    return null;
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Not found key with uid={} for {}", uid, socketAddress);
                }
            }
        }
        return cipher;
    }
}
