package com.omgameserver.engine.udp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.core.events.CoreTickEvent;
import com.omgameserver.engine.udp.events.UdpClientDisconnectedEvent;
import com.omgameserver.engine.udp.events.UdpIncomingHeaderEvent;
import com.omgameserver.engine.udp.events.UdpOutgoingPayloadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class UdpOutputService extends Bolt implements
        UdpIncomingHeaderEvent.Handler,
        UdpOutgoingPayloadEvent.Handler,
        UdpClientDisconnectedEvent.Handler,
        CoreTickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(UdpOutputService.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final UdpProperties properties;
    private final Map<SocketAddress, UdpOutputClient> clientBySocket;
    private final Map<Long, UdpOutputClient> clientByUid;

    UdpOutputService(CoreExecutors executors, CoreDispatcher dispatcher, UdpProperties properties) {
        super("output", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.properties = properties;
        clientBySocket = new HashMap<>();
        clientByUid = new HashMap<>();
    }

    @Override
    public void handleUdpIncomingHeader(UdpIncomingHeaderEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        long clientUid = event.getClientUid();
        UdpOutputClient udpOutputClient = clientBySocket.get(socketAddress);
        if (udpOutputClient == null) {
            udpOutputClient = new UdpOutputClient(properties, dispatcher, socketAddress, clientUid);
            clientBySocket.put(socketAddress, udpOutputClient);
            clientByUid.put(clientUid, udpOutputClient);
            logger.debug("New output client for {} with uid={}", socketAddress, clientUid);
        }
        udpOutputClient.handleHeader(event.getSeq(), event.getAck(), event.getBit(), event.getSys());
    }

    @Override
    public void handleUdpOutgoingPayload(UdpOutgoingPayloadEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        UdpOutputClient udpOutputClient = clientByUid.get(clientUid);
        if (udpOutputClient != null) {
            udpOutputClient.send(event);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Not found client for {}", clientUid);
            }
        }
    }

    @Override
    public void handleUdpClientDisconnected(UdpClientDisconnectedEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        long clientUid = event.getClientUid();
        UdpOutputClient removedClient = clientBySocket.remove(socketAddress);
        if (removedClient != null) {
            logger.debug("{} removed", removedClient);
        }
        clientByUid.remove(clientUid);
    }

    @Override
    public void handleCoreTick(CoreTickEvent event) throws InterruptedException {
        long currentTimeMillis = System.currentTimeMillis();
        Iterator<Map.Entry<SocketAddress, UdpOutputClient>> iterator = clientBySocket.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SocketAddress, UdpOutputClient> entry = iterator.next();
            UdpOutputClient client = entry.getValue();
            if (client.isPingTime(currentTimeMillis)) {
                client.ping();
            }
            client.flush();
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, UdpIncomingHeaderEvent.class);
        dispatcher.getDispatcher().subscribe(this, UdpOutgoingPayloadEvent.class);
        dispatcher.getDispatcher().subscribe(this, UdpClientDisconnectedEvent.class);
        dispatcher.getDispatcher().subscribe(this, CoreTickEvent.class);
    }
}
