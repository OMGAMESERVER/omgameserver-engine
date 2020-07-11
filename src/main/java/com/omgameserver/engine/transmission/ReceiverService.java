package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Worker;
import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.OmgsExecutors;
import com.omgameserver.engine.events.IncomingDatagramEvent;
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
class ReceiverService extends Worker {
    static private final Logger logger = LoggerFactory.getLogger(ReceiverService.class);

    private final OmgsExecutors executors;
    private final OmgsDispatcher dispatcher;
    private final Channel.Receiver receiver;

    ReceiverService(OmgsExecutors executors, OmgsDispatcher dispatcher,
                    Channel channel) {
        super();
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.receiver = channel.getReceiver();
    }

    @Override
    public void run() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("receiver-" + uid);
        logger.info("{} started", this);
        try {
            looping = true;
            while (looping) {
                IncomingDatagramEvent incomingDatagramEvent = receiver.receive();
                dispatcher.getDispatcher().dispatch(incomingDatagramEvent);
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
