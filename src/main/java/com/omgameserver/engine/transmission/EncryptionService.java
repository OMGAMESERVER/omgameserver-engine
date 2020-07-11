package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.OmgsExecutors;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.ClientDisconnectedEvent;
import com.omgameserver.engine.events.OutgoingDatagramEvent;
import com.omgameserver.engine.events.OutgoingRawDataEvent;
import com.omgameserver.engine.events.SecretKeyAssignedEvent;
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
class EncryptionService extends Bolt implements
        SecretKeyAssignedEvent.Handler,
        OutgoingRawDataEvent.Handler,
        ClientDisconnectedEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private final OmgsExecutors executors;
    private final OmgsDispatcher dispatcher;
    private final Map<SocketAddress, Cipher> ciphers;

    EncryptionService(OmgsProperties properties, OmgsExecutors executors, OmgsDispatcher dispatcher) {
        super("encryptor", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        ciphers = new HashMap<>();
    }

    @Override
    public void handleSecretKeyAssigned(SecretKeyAssignedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SecretKey secretKey = event.getSecretKey();
        SocketAddress socketAddress = event.getSocketAddress();
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            ciphers.put(socketAddress, cipher);
            if (logger.isDebugEnabled()) {
                logger.debug("Encrypt cipher created for {}", socketAddress);
            }
        } catch (GeneralSecurityException e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public void handleOutgoingRawData(OutgoingRawDataEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getTargetAddress();
        ByteBuffer rawData = event.getRawData();
        Cipher cipher = ciphers.get(socketAddress);
        if (cipher != null) {
            try {
                int outputSize = cipher.getOutputSize(rawData.remaining());
                ByteBuffer datagram = ByteBuffer.allocate(outputSize);
                cipher.doFinal(rawData, datagram);
                datagram.flip();
                dispatcher.getDispatcher().dispatch(new OutgoingDatagramEvent(socketAddress, datagram));
            } catch (GeneralSecurityException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Decryption failed for {} as {}", socketAddress, e);
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Cipher not found for {}", socketAddress);
            }
        }
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
        dispatcher.getDispatcher().subscribe(this, SecretKeyAssignedEvent.class);
        dispatcher.getDispatcher().subscribe(this, OutgoingRawDataEvent.class);
        dispatcher.getDispatcher().subscribe(this, ClientDisconnectedEvent.class);
    }
}
