package com.omgameserver.engine.transmission;

import com.omgameserver.engine.OmgsConstants;
import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.OmgsProperties;
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

    private OmgsProperties properties;
    private OmgsDispatcher dispatcher;
    private SocketAddress socketAddress;
    private long clientUid;
    private long lastActivity;

    InputClient(OmgsProperties properties, OmgsDispatcher dispatcher, SocketAddress socketAddress) {
        super();
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.socketAddress = socketAddress;
        clientUid = uidCounter.incrementAndGet();
        lastActivity = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return InputClient.class.getSimpleName() + "(clientUid=" + clientUid +
                ", socket=" + socketAddress.toString() + ")";
    }

    SocketAddress getSocketAddress() {
        return socketAddress;
    }

    long getClientUid() {
        return clientUid;
    }

    boolean handleDatagram(ByteBuffer byteBuffer) throws InterruptedException {
        if (byteBuffer.remaining() < HEADER_SIZE) {
            if (logger.isDebugEnabled()) {
                logger.debug("Wrong header's length={} from {}", byteBuffer.remaining(), socketAddress);
            }
            return false;
        } else {
            int seq = byteBuffer.getInt();
            int ack = byteBuffer.getInt();
            int bit = byteBuffer.getInt();
            int sys = byteBuffer.get();
            lastActivity = System.currentTimeMillis();
            dispatcher.getDispatcher().dispatch(new IncomingHeaderEvent(socketAddress, clientUid, seq, ack, bit, sys));
            if (byteBuffer.remaining() > 0) {
                ByteBuffer payload = ByteBuffer.allocate(byteBuffer.remaining());
                payload.put(byteBuffer);
                payload.flip();
                dispatcher.getDispatcher().dispatch(new IncomingPayloadEvent(clientUid, payload));
            }
            return true;
        }
    }

    boolean isDisconnected(long currentTimeMillis) {
        return currentTimeMillis - lastActivity > properties.getDisconnectInterval();
    }
}