package com.omgameserver.engine.transmission;

import com.omgameserver.engine.OmgsConstants;
import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.ClientAccessRequestEvent;
import com.omgameserver.engine.events.IncomingHeaderEvent;
import com.omgameserver.engine.events.IncomingPayloadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class InputClient implements OmgsConstants {
    static private final Logger logger = LoggerFactory.getLogger(InputClient.class);
    static private final AtomicLong uidCounter = new AtomicLong();

    private final OmgsProperties properties;
    private final OmgsDispatcher dispatcher;
    private final SocketAddress socketAddress;
    private final long clientUid;
    private long lastActivity;
    private boolean hasAccess;

    InputClient(OmgsProperties properties, OmgsDispatcher dispatcher, SocketAddress socketAddress) {
        super();
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.socketAddress = socketAddress;
        clientUid = uidCounter.incrementAndGet();
        lastActivity = System.currentTimeMillis();
        hasAccess = false;
    }

    @Override
    public String toString() {
        return InputClient.class.getSimpleName() + "(clientUid=" + clientUid +
                ", socket=" + socketAddress.toString() + ", hasAccess=" + hasAccess + ")";
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
        } else {
            int seq = datagram.getInt();
            int ack = datagram.getInt();
            int bit = datagram.getInt();
            int sys = datagram.get();
            lastActivity = System.currentTimeMillis();
            dispatcher.getDispatcher().dispatch(new IncomingHeaderEvent(socketAddress, clientUid, seq, ack, bit, sys));
            return handleRawData(datagram);
        }
        return false;
    }

    void grantAccess() {
        hasAccess = true;
        if (logger.isDebugEnabled()) {
            logger.debug("Client from socketAddress={} with clientUid={} got access", clientUid, socketAddress);
        }
    }

    boolean isDisconnected(long currentTimeMillis) {
        return currentTimeMillis - lastActivity > properties.getDisconnectInterval();
    }

    private boolean handleRawData(ByteBuffer datagram) throws InterruptedException {
        if (hasAccess) {
            return handlePayload(datagram);
        } else {
            return handleAccessRequest(datagram);
        }
    }

    private boolean handlePayload(ByteBuffer byteBuffer) throws InterruptedException {
        if (byteBuffer.remaining() > 0) {
            ByteBuffer payload = ByteBuffer.allocate(byteBuffer.remaining());
            payload.put(byteBuffer);
            payload.flip();
            dispatcher.getDispatcher().dispatch(new IncomingPayloadEvent(clientUid, payload));
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Datagram from {} has no payload", socketAddress);
            }
        }
        return true;
    }

    private boolean handleAccessRequest(ByteBuffer byteBuffer) throws InterruptedException {
        if (byteBuffer.remaining() >= Long.BYTES) {
            Long accessKey = byteBuffer.getLong();
            dispatcher.getDispatcher().dispatch(new ClientAccessRequestEvent(socketAddress, clientUid, accessKey));
            return true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Wrong access key from {}", socketAddress);
            }
            return false;
        }
    }
}