package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsExecutors;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.ClientDisconnectedEvent;
import com.omgameserver.engine.events.DisconnectClientRequestEvent;
import com.omgameserver.engine.events.IncomingRawDataEvent;
import com.omgameserver.engine.events.TickEvent;
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
        IncomingRawDataEvent.Handler,
        DisconnectClientRequestEvent.Handler,
        TickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(InputService.class);

    private final OmgsProperties properties;
    private final OmgsExecutors executors;
    private final Dispatcher dispatcher;
    private final Map<SocketAddress, InputClient> clientBySocket;
    private final Map<Long, InputClient> clientByUid;

    InputService(OmgsProperties properties, OmgsExecutors executors, Dispatcher dispatcher) {
        super("input", properties.getQueueSize());
        this.properties = properties;
        this.executors = executors;
        this.dispatcher = dispatcher;
        clientBySocket = new HashMap<>();
        clientByUid = new HashMap<>();
    }

    @Override
    public void handleIncomingRawData(IncomingRawDataEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        ByteBuffer rawData = event.getRawData();
        InputClient inputClient = clientBySocket.get(socketAddress);
        if (inputClient == null) {
            inputClient = new InputClient(properties, dispatcher, socketAddress);
            clientBySocket.put(socketAddress, inputClient);
            clientByUid.put(inputClient.getClientUid(), inputClient);
            if (logger.isInfoEnabled()) {
                logger.info("New input client from {} with uid={}", socketAddress, inputClient.getClientUid());
            }
        }
        inputClient.handleDatagram(rawData);
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
            dispatcher.dispatch(new ClientDisconnectedEvent(inputClient.getSocketAddress()));
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
                dispatcher.dispatch(new ClientDisconnectedEvent(inputClient.getSocketAddress()));
                logger.info("{} timed out", inputClient);
            }
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.subscribe(this, IncomingRawDataEvent.class);
        dispatcher.subscribe(this, DisconnectClientRequestEvent.class);
        dispatcher.subscribe(this, TickEvent.class);
    }
}
