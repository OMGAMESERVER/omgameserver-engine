package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.OutgoingDatagramEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class SendingService extends Bolt implements OutgoingDatagramEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(SendingService.class);

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;
    private final Channel.Sender sender;

    SendingService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher,
                   Channel channel) {
        super("sender", properties.getQueueSize());
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
        this.sender = channel.getSender();
    }

    @Override
    public void handleOutgoingDatagram(OutgoingDatagramEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        try {
            sender.send(event);
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Send to " + event.getTargetAddress() + " failed", e);
            }
        }
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, OutgoingDatagramEvent.class);
    }
}
