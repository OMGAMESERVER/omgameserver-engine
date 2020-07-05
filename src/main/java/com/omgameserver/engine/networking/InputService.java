package com.omgameserver.engine.networking;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.IncomingDatagramEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class InputService extends Bolt implements
        IncomingDatagramEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(InputService.class);

    private final OmgsProperties properties;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;
    private final State state;

    InputService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("input", properties.getQueueSize());
        this.properties = properties;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
        this.state = new State();
    }

    @Override
    public void handleIncomingDatagram(IncomingDatagramEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress sourceAddress = event.getSourceAddress();
        ByteBuffer byteBuffer = event.getByteBuffer();
        InputClient inputClient = state.getClientOrCreate(sourceAddress);
        inputClient.handleDatagram(byteBuffer);
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, IncomingDatagramEvent.class);
    }

    private class State {

        private final Map<SocketAddress, InputClient> clientBySocket;
        private final Map<Long, InputClient> clientByUid;

        State() {
            clientBySocket = new HashMap<>();
            clientByUid = new HashMap<>();
        }

        InputClient getClientOrCreate(SocketAddress socketAddress) {
            InputClient client = clientBySocket.get(socketAddress);
            if (client == null) {
                client = new InputClient(properties, dispatcher, socketAddress);
                clientBySocket.put(socketAddress, client);
                clientByUid.put(client.getClientUid(), client);
                if (logger.isInfoEnabled()) {
                    logger.info("New input client from {} with uid={}", socketAddress, client.getClientUid());
                }
            }
            return client;
        }
    }
}
