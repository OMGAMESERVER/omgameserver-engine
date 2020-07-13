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
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class AccessService extends Bolt implements
        AccessKeyCreatedEvent.Handler,
        ClientAccessRequestEvent.Handler,
        TickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(AccessService.class);

    private final OmgsProperties properties;
    private final OmgsExecutors executors;
    private final OmgsDispatcher dispatcher;
    // Map accessKey to createionTime
    private final Map<Long, Long> temporaryKeys;

    AccessService(OmgsProperties properties, OmgsExecutors executors, OmgsDispatcher dispatcher) {
        super("access", properties.getQueueSize());
        this.properties = properties;
        this.executors = executors;
        this.dispatcher = dispatcher;
        temporaryKeys = new HashMap<>();
    }

    @Override
    public void handleAccessKeyCreated(AccessKeyCreatedEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long secretKey = event.getAccessKey();
        // TODO: handle case when not unique keys generated
        temporaryKeys.put(secretKey, System.currentTimeMillis());
    }

    @Override
    public void handleClientAccessRequest(ClientAccessRequestEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        long clientUid = event.getClientUid();
        long accessKey = event.getAccessKey();
        if (temporaryKeys.remove(accessKey) != null) {
            dispatcher.getDispatcher().dispatch(new GrantAccessToClient(socketAddress, clientUid));
        } else {
            dispatcher.getDispatcher().dispatch(new DisconnectClientRequestEvent(clientUid));
            if (logger.isDebugEnabled()) {
                logger.debug("Unknown accessKey={} from {}, access forbidden and client will be disconnected",
                        accessKey, socketAddress);
            }
        }
    }

    @Override
    public void handleTick(TickEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long currentTimeMillis = System.currentTimeMillis();
        long lifeTime = properties.getAccessKeyLifetime();
        Iterator<Map.Entry<Long, Long>> iterator = temporaryKeys.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            long accessKey = entry.getKey();
            long creationTime = entry.getValue();
            if ((currentTimeMillis - creationTime) >= lifeTime) {
                if (logger.isDebugEnabled()) {
                    logger.debug("AccessKey={} expired", accessKey);
                }
                dispatcher.getDispatcher().dispatch(new AccessKeyExpiredEvent(accessKey));
                iterator.remove();
            }
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, AccessKeyCreatedEvent.class);
        dispatcher.getDispatcher().subscribe(this, ClientAccessRequestEvent.class);
        dispatcher.getDispatcher().subscribe(this, TickEvent.class);
    }
}
