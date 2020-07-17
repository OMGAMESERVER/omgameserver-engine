package com.omgameserver.engine.udp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.core.events.CoreTickEvent;
import com.omgameserver.engine.udp.events.UdpClientConnectedEvent;
import com.omgameserver.engine.udp.events.UdpClientDisconnectedEvent;
import com.omgameserver.engine.udp.events.UdpIncomingDatagramEvent;
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
class UdpInputService extends Bolt implements
        UdpIncomingDatagramEvent.Handler,
        CoreTickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(UdpInputService.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final UdpProperties properties;
    private final Map<SocketAddress, UdpInputClient> clientBySocket;
    private final Map<Long, UdpInputClient> clientByUid;

    UdpInputService(CoreExecutors executors, CoreDispatcher dispatcher, UdpProperties properties) {
        super("input", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.properties = properties;
        clientBySocket = new HashMap<>();
        clientByUid = new HashMap<>();
    }

    @Override
    public void handleUdpIncomingDatagram(UdpIncomingDatagramEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress socketAddress = event.getSocketAddress();
        ByteBuffer byteBuffer = event.getByteBuffer();
        UdpInputClient udpInputClient = clientBySocket.get(socketAddress);
        if (udpInputClient == null) {
            udpInputClient = new UdpInputClient(properties, dispatcher, socketAddress);
            long clientUid = udpInputClient.getClientUid();
            clientBySocket.put(socketAddress, udpInputClient);
            clientByUid.put(udpInputClient.getClientUid(), udpInputClient);
            if (logger.isInfoEnabled()) {
                logger.info("New input client from {} with uid={} created", socketAddress, clientUid);
            }
            dispatcher.dispatch(new UdpClientConnectedEvent(socketAddress, clientUid));
        }
        udpInputClient.handleDatagram(byteBuffer);
    }

    @Override
    public void handleCoreTick(CoreTickEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long currentTimeMillis = System.currentTimeMillis();
        Iterator<Map.Entry<SocketAddress, UdpInputClient>> iterator = clientBySocket.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SocketAddress, UdpInputClient> entry = iterator.next();
            UdpInputClient udpInputClient = entry.getValue();
            if (udpInputClient.isDisconnected(currentTimeMillis)) {
                clientByUid.remove(udpInputClient.getClientUid());
                iterator.remove();
                dispatcher.dispatch(
                        new UdpClientDisconnectedEvent(udpInputClient.getSocketAddress(), udpInputClient.getClientUid()));
                logger.info("{} timed out", udpInputClient);
            }
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, UdpIncomingDatagramEvent.class);
        dispatcher.getDispatcher().subscribe(this, CoreTickEvent.class);
    }
}
