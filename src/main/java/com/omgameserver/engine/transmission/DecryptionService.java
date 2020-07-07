package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.IncomingDatagramEvent;
import com.omgameserver.engine.events.IncomingRawDataEvent;
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
public class DecryptionService extends Bolt implements
        IncomingDatagramEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(DecryptionService.class);

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;

    DecryptionService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("decryptor", properties.getQueueSize());
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
    }

    @Override
    public void handleIncomingDatagram(IncomingDatagramEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        ByteBuffer byteBuffer = event.getByteBuffer();
        long keyUid = byteBuffer.getLong();
        // TODO: decrypt using key
        ByteBuffer rawData = ByteBuffer.allocate(byteBuffer.remaining());
        rawData.put(byteBuffer);
        rawData.flip();
        dispatcher.dispatch(new IncomingRawDataEvent(socketAddress, rawData));
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, IncomingDatagramEvent.class);
    }
}
