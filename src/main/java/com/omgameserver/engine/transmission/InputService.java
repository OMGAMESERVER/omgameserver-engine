package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.EngineDispatcher;
import com.omgameserver.engine.EngineExecutors;
import com.omgameserver.engine.EngineProperties;
import com.omgameserver.engine.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class InputService extends Bolt implements
        IncomingDatagramEvent.Handler,
        DisconnectClientRequestEvent.Handler,
        TickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(InputService.class);

    private final EngineProperties properties;
    private final EngineExecutors executors;
    private final EngineDispatcher dispatcher;
    private final Map<SocketAddress, InputClient> clientBySocket;
    private final Map<Long, InputClient> clientByUid;

    InputService(EngineProperties properties, EngineExecutors executors, EngineDispatcher dispatcher) {
        super("input", properties.getQueueSize());
        this.properties = properties;
        this.executors = executors;
        this.dispatcher = dispatcher;
        clientBySocket = new HashMap<>();
        clientByUid = new HashMap<>();
    }

    @Override
    public void handleIncomingDatagram(IncomingDatagramEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        ByteBuffer byteBuffer = event.getByteBuffer();
        InputClient inputClient = clientBySocket.get(socketAddress);
        if (inputClient == null) {
            inputClient = new InputClient(properties, dispatcher, socketAddress);
            long clientUid = inputClient.getClientUid();
            clientBySocket.put(socketAddress, inputClient);
            clientByUid.put(inputClient.getClientUid(), inputClient);
            if (logger.isInfoEnabled()) {
                logger.info("New input client from {} with uid={} created", socketAddress, clientUid);
            }
            dispatcher.dispatch(new ClientConnectedEvent(socketAddress, clientUid));
        }
        if (!inputClient.handleDatagram(byteBuffer)) {
            dispatcher.dispatch(new DisconnectClientRequestEvent(inputClient.getClientUid()));
        }
    }

    @Override
    public void handleDisconnectClientRequest(DisconnectClientRequestEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        InputClient inputClient = clientByUid.get(clientUid);
        if (inputClient != null) {
            clientByUid.remove(clientUid);
            SocketAddress socketAddress = inputClient.getSocketAddress();
            clientBySocket.remove(socketAddress);
            dispatcher.dispatch(new ClientDisconnectedEvent(inputClient.getSocketAddress(), clientUid));
            logger.info("{} disconnected by server", inputClient);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Client with uid={} to disconnect not found", clientUid);
            }
        }
    }

    @Override
    public void handleTick(TickEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long currentTimeMillis = System.currentTimeMillis();
        Iterator<Map.Entry<SocketAddress, InputClient>> iterator = clientBySocket.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SocketAddress, InputClient> entry = iterator.next();
            InputClient inputClient = entry.getValue();
            if (inputClient.isDisconnected(currentTimeMillis)) {
                clientByUid.remove(inputClient.getClientUid());
                iterator.remove();
                dispatcher.dispatch(
                        new ClientDisconnectedEvent(inputClient.getSocketAddress(), inputClient.getClientUid()));
                logger.info("{} timed out", inputClient);
            }
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, IncomingDatagramEvent.class);
        dispatcher.getDispatcher().subscribe(this, DisconnectClientRequestEvent.class);
        dispatcher.getDispatcher().subscribe(this, TickEvent.class);
    }
}
