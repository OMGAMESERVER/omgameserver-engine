package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.EngineDispatcher;
import com.omgameserver.engine.EngineExecutors;
import com.omgameserver.engine.EngineProperties;
import com.omgameserver.engine.events.OutgoingDatagramEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class SenderService extends Bolt implements OutgoingDatagramEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(SenderService.class);

    private final EngineExecutors executors;
    private final EngineDispatcher dispatcher;
    private final Channel.Sender sender;

    SenderService(EngineProperties properties, EngineExecutors executors, EngineDispatcher dispatcher,
                  Channel channel) {
        super("sender", properties.getQueueSize());
        this.executors = executors;
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
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, OutgoingDatagramEvent.class);
    }
}
