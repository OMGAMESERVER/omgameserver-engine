package com.omgameserver.engine.utils;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.OmgsExecutors;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.SecretKeyAssignedEvent;
import com.omgameserver.engine.events.SecretKeyCreatedEvent;
import com.omgameserver.engine.events.SecretKeyExpiredEvent;
import com.omgameserver.engine.events.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class KeysTrackerService extends Bolt implements
        SecretKeyCreatedEvent.Handler,
        SecretKeyAssignedEvent.Handler,
        TickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(KeysTrackerService.class);

    private final OmgsProperties properties;
    private final OmgsExecutors executors;
    private final OmgsDispatcher dispatcher;
    // Map for keyUid => createionTime
    private final Map<Long, Long> temporaryKeys;

    KeysTrackerService(OmgsProperties properties, OmgsExecutors executors, OmgsDispatcher dispatcher) {
        super("keys-tracker", properties.getQueueSize());
        this.properties = properties;
        this.executors = executors;
        this.dispatcher = dispatcher;
        temporaryKeys = new HashMap<>();
    }

    @Override
    public void handleSecretKeyCreated(SecretKeyCreatedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long keyUid = event.getKeyUid();
        temporaryKeys.put(keyUid, System.currentTimeMillis());
    }

    @Override
    public void handleSecretKeyAssigned(SecretKeyAssignedEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        temporaryKeys.remove(event.getKeyUid());
    }

    @Override
    public void handleTick(TickEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long currentTimeMillis = System.currentTimeMillis();
        long lifeTime = properties.getSecretKeyLifetime();
        Iterator<Map.Entry<Long, Long>> iterator = temporaryKeys.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            long keyUid = entry.getKey();
            long creationTime = entry.getValue();
            if ((currentTimeMillis - creationTime) >= lifeTime) {
                dispatcher.getDispatcher().dispatch(new SecretKeyExpiredEvent(keyUid));
                iterator.remove();
            }
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, SecretKeyCreatedEvent.class);
        dispatcher.getDispatcher().subscribe(this, SecretKeyAssignedEvent.class);
        dispatcher.getDispatcher().subscribe(this, TickEvent.class);
    }
}
