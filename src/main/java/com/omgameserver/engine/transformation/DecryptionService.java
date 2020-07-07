package com.omgameserver.engine.transformation;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.IncomingPayloadEvent;
import com.omgameserver.engine.events.IncomingRawDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
public class DecryptionService extends Bolt implements
        IncomingPayloadEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(DecryptionService.class);

    private final OmgsProperties properties;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;

    DecryptionService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("decryptor", properties.getQueueSize());
        this.properties = properties;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
    }

    @Override
    public void handleIncomingPayload(IncomingPayloadEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        ByteBuffer payload = event.getPayload();
        // TODO: decrypt payload
        ByteBuffer rawData = ByteBuffer.allocate(payload.remaining());
        rawData.put(payload);
        dispatcher.dispatch(new IncomingRawDataEvent(clientUid, rawData));
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, IncomingPayloadEvent.class);
    }
}
