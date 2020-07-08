package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.ClientDisconnectedEvent;
import com.omgameserver.engine.events.OutgoingDatagramEvent;
import com.omgameserver.engine.events.OutgoingRawDataEvent;
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
class EncryptionService extends Bolt implements
        SecretKeyAssignedEvent.Handler,
        OutgoingRawDataEvent.Handler,
        ClientDisconnectedEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;
    private final Map<SocketAddress, SecretKey> assignedKeys;

    EncryptionService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("encryptor", properties.getQueueSize());
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
        assignedKeys = new ConcurrentHashMap<>();
    }

    @Override
    public void handleSecretKeyAssigned(SecretKeyAssignedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SecretKey secretKey = event.getSecretKey();
        SocketAddress socketAddress = event.getSocketAddress();
        assignedKeys.put(socketAddress, secretKey);
    }

    @Override
    public void handleOutgoingRawData(OutgoingRawDataEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getTargetAddress();
        ByteBuffer rawData = event.getRawData();
        SecretKey secretKey = assignedKeys.get(socketAddress);
        if (secretKey != null) {
            // TODO: encrypt rawData by secretKey
            ByteBuffer datagram = ByteBuffer.allocate(rawData.remaining());
            datagram.put(rawData);
            dispatcher.dispatch(new OutgoingDatagramEvent(socketAddress, datagram));
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Secret key not found for {}", socketAddress);
            }
        }
    }

    @Override
    public void handleClientDisconnected(ClientDisconnectedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        assignedKeys.remove(event.getSocketAddress());
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, SecretKeyAssignedEvent.class);
        dispatcher.subscribe(this, OutgoingRawDataEvent.class);
        dispatcher.subscribe(this, ClientDisconnectedEvent.class);
    }
}
