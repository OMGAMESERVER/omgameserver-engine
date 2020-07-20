package com.omgameserver.engine.udp;

import com.crionuke.bolts.Worker;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.udp.events.UdpIncomingDatagramEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class UdpReceiverService extends Worker {
    static private final Logger logger = LoggerFactory.getLogger(UdpReceiverService.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final UdpChannel.Receiver receiver;

    UdpReceiverService(CoreExecutors executors, CoreDispatcher dispatcher,
                       UdpChannel udpChannel) {
        super();
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.receiver = udpChannel.getReceiver();
    }

    @Override
    public void run() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("receiver-" + uid);
        logger.info("{} started", this);
        try {
            looping = true;
            while (looping) {
                UdpIncomingDatagramEvent udpIncomingDatagramEvent = receiver.receive();
                dispatcher.dispatch(udpIncomingDatagramEvent);
            }
        } catch (InterruptedException | AsynchronousCloseException e) {
            logger.debug("{} interrupted", this);
            looping = false;
        } catch (IOException ioe) {
            logger.warn("{}", ioe);
            looping = false;
        }
        logger.info("{} finished", this);
        Thread.currentThread().setName(oldThreadName);
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
    }
}
