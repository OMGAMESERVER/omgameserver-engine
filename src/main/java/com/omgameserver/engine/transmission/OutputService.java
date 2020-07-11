package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsExecutors;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.ClientDisconnectedEvent;
import com.omgameserver.engine.events.IncomingHeaderEvent;
import com.omgameserver.engine.events.OutgoingPayloadEvent;
import com.omgameserver.engine.events.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class OutputService extends Bolt implements
        IncomingHeaderEvent.Handler,
        OutgoingPayloadEvent.Handler,
        ClientDisconnectedEvent.Handler,
        TickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(OutputService.class);

    private final OmgsProperties properties;
    private final OmgsExecutors executors;
    private final Dispatcher dispatcher;
    private final Map<SocketAddress, OutputClient> clientBySocket;
    private final Map<Long, OutputClient> clientByUid;

    OutputService(OmgsProperties properties, OmgsExecutors executors, Dispatcher dispatcher) {
        super("output", properties.getQueueSize());
        this.properties = properties;
        this.executors = executors;
        this.dispatcher = dispatcher;
        clientBySocket = new HashMap<>();
        clientByUid = new HashMap<>();
    }

    @Override
    public void handleIncomingHeader(IncomingHeaderEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        long clientUid = event.getClientUid();
        OutputClient outputClient = clientBySocket.get(socketAddress);
        if (outputClient == null) {
            outputClient = new OutputClient(properties, dispatcher, socketAddress, clientUid);
            clientBySocket.put(socketAddress, outputClient);
            clientByUid.put(clientUid, outputClient);
            logger.debug("New output client for {} with uid={}", socketAddress, clientUid);
        }
        outputClient.handleHeader(event.getSeq(), event.getAck(), event.getBit(), event.getSys());
    }

    @Override
    public void handleOutgoingPayload(OutgoingPayloadEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        OutputClient outputClient = clientByUid.get(clientUid);
        if (outputClient != null) {
            outputClient.send(event);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Not found client for {}", clientUid);
            }
        }
    }

    @Override
    public void handleClientDisconnected(ClientDisconnectedEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        OutputClient removedClient = clientBySocket.remove(socketAddress);
        if (removedClient != null) {
            logger.debug("{} removed", removedClient);
        }
        clientByUid.remove(removedClient.getClientUid());
    }

    @Override
    public void handleTick(TickEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long currentTimeMillis = System.currentTimeMillis();
        Iterator<Map.Entry<SocketAddress, OutputClient>> iterator = clientBySocket.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SocketAddress, OutputClient> entry = iterator.next();
            OutputClient client = entry.getValue();
            if (client.isPingTime(currentTimeMillis)) {
                client.ping();
            }
            client.flush();
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.subscribe(this, IncomingHeaderEvent.class);
        dispatcher.subscribe(this, OutgoingPayloadEvent.class);
        dispatcher.subscribe(this, ClientDisconnectedEvent.class);
        dispatcher.subscribe(this, TickEvent.class);
    }
}
