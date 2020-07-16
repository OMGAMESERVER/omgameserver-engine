package com.omgameserver.engine.udp;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.udp.events.UdpIncomingHeaderEvent;
import com.omgameserver.engine.udp.events.UdpIncomingPayloadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class UdpInputClientConstants implements UdpHeaderConstants {
    static private final Logger logger = LoggerFactory.getLogger(UdpInputClientConstants.class);
    static private final AtomicLong uidCounter = new AtomicLong();

    private final UdpProperties properties;
    private final CoreDispatcher dispatcher;
    private final SocketAddress socketAddress;
    private final long clientUid;
    private long lastActivity;

    UdpInputClientConstants(UdpProperties properties, CoreDispatcher dispatcher, SocketAddress socketAddress) {
        super();
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.socketAddress = socketAddress;
        clientUid = uidCounter.incrementAndGet();
        lastActivity = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return UdpInputClientConstants.class.getSimpleName() + "(clientUid=" + clientUid +
                ", socket=" + socketAddress.toString() + ")";
    }

    SocketAddress getSocketAddress() {
        return socketAddress;
    }

    long getClientUid() {
        return clientUid;
    }

    boolean handleDatagram(ByteBuffer datagram) throws InterruptedException {
        if (datagram.remaining() < HEADER_SIZE) {
            if (logger.isDebugEnabled()) {
                logger.debug("Wrong header's length={} from {}", datagram.remaining(), socketAddress);
            }
            return false;
        } else {
            int seq = datagram.getInt();
            int ack = datagram.getInt();
            int bit = datagram.getInt();
            int sys = datagram.get();
            lastActivity = System.currentTimeMillis();
            dispatcher.dispatch(new UdpIncomingHeaderEvent(socketAddress, clientUid, seq, ack, bit, sys));
            if (datagram.remaining() > 0) {
                ByteBuffer payload = ByteBuffer.allocate(datagram.remaining());
                payload.put(datagram);
                payload.flip();
                dispatcher.dispatch(new UdpIncomingPayloadEvent(socketAddress, clientUid, payload));
            }
            return true;
        }
    }

    boolean isDisconnected(long currentTimeMillis) {
        return currentTimeMillis - lastActivity > properties.getDisconnectInterval();
    }
}