package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.OutgoingDatagramEvent;
import com.omgameserver.engine.events.OutgoingRawDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
public class EncryptionService extends Bolt implements
        OutgoingRawDataEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;

    EncryptionService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("encryptor", properties.getQueueSize());
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
    }

    @Override
    public void handleOutgoingRawData(OutgoingRawDataEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getTargetAddress();
        ByteBuffer rawData = event.getRawData();
        // TODO: encrypt rawData
        ByteBuffer datagram = ByteBuffer.allocate(rawData.remaining());
        datagram.put(rawData);
        dispatcher.dispatch(new OutgoingDatagramEvent(socketAddress, datagram));
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, OutgoingRawDataEvent.class);
    }
}
