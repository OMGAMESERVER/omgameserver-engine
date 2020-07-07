package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.AuthorizationCreatedEvent;
import com.omgameserver.engine.events.IncomingDatagramEvent;
import com.omgameserver.engine.events.IncomingRawDataEvent;
import com.omgameserver.engine.events.SecretKeyAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
public class DecryptionService extends Bolt implements
        AuthorizationCreatedEvent.Handler,
        IncomingDatagramEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(DecryptionService.class);

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;
    private final Map<Long, SecretKey> temporaryKeys;
    private final Map<SocketAddress, SecretKey> assignedKeys;


    DecryptionService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("decryptor", properties.getQueueSize());
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
        temporaryKeys = new ConcurrentHashMap<>();
        assignedKeys = new ConcurrentHashMap<>();
    }

    @Override
    public void handleAuthorizationCreated(AuthorizationCreatedEvent event) {
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
        SecretKey secretKey = assignedKeys.get(socketAddress);
        if (secretKey == null) {
            secretKey = temporaryKeys.remove(keyUid);
            if (secretKey != null) {
                assignedKeys.put(socketAddress, secretKey);
                dispatcher.dispatch(new SecretKeyAssignedEvent(secretKey, socketAddress));
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unknown key uid={} got from {}", keyUid, socketAddress);
                }
                return;
            }
        }
        // TODO: decrypt using secretKey
        ByteBuffer rawData = ByteBuffer.allocate(byteBuffer.remaining());
        rawData.put(byteBuffer);
        rawData.flip();
        dispatcher.dispatch(new IncomingRawDataEvent(socketAddress, rawData));
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, AuthorizationCreatedEvent.class);
        dispatcher.subscribe(this, IncomingDatagramEvent.class);
    }
}
