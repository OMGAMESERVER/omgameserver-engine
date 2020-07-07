package com.omgameserver.engine.transformation;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.OutgoingPayloadEvent;
import com.omgameserver.engine.events.OutgoingRawDataEvent;
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
public class EncryptionService extends Bolt implements
        OutgoingRawDataEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private final OmgsProperties properties;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;

    EncryptionService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("encryptor", properties.getQueueSize());
        this.properties = properties;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
    }

    @Override
    public void handleOutgoingRawData(OutgoingRawDataEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        ByteBuffer rawData = event.getRawData();
        boolean ephemeral = event.isEphemeral();
        // TODO: encrypt rawData
        ByteBuffer payload = ByteBuffer.allocate(rawData.remaining());
        payload.put(rawData);
        dispatcher.dispatch(new OutgoingPayloadEvent(clientUid, payload, ephemeral));
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, OutgoingRawDataEvent.class);
    }
}
