package com.omgameserver.engine.udp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.udp.events.UdpOutgoingDatagramEvent;
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
class UdpSenderService extends Bolt implements UdpOutgoingDatagramEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(UdpSenderService.class);

    private final UdpProperties properties;
    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final UdpChannelConstants.Sender sender;

    UdpSenderService(CoreExecutors executors, CoreDispatcher dispatcher, UdpProperties properties,
                     UdpChannelConstants udpChannel) {
        super("sender", properties.getQueueSize());
        this.properties = properties;
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.sender = udpChannel.getSender();
    }

    @Override
    public void handleUdpOutgoingDatagram(UdpOutgoingDatagramEvent event) {
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
        dispatcher.getDispatcher().subscribe(this, UdpOutgoingDatagramEvent.class);
    }
}
