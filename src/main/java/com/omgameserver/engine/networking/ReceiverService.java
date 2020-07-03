package com.omgameserver.engine.networking;

import com.crionuke.bolts.Dispatcher;
import com.crionuke.bolts.Worker;
import com.omgameserver.engine.events.IncomingDatagramEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class ReceiverService extends Worker {
    static private final Logger logger = LoggerFactory.getLogger(ReceiverService.class);

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;
    private final Channel.Receiver receiver;

    ReceiverService(ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher,
                    Channel channel) {
        super();
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
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
                dispatcher.dispatch(incomingDatagramEvent);
            }
        } catch (InterruptedException | ClosedByInterruptException e) {
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
        threadPoolTaskExecutor.execute(this);
    }
}
