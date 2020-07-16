package com.omgameserver.engine.udp.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class UdpIncomingPayloadEvent extends Event<UdpIncomingPayloadEvent.Handler> {

    private final SocketAddress socketAddress;
    private final long clientUid;
    private final ByteBuffer payload;

    public UdpIncomingPayloadEvent(SocketAddress socketAddress, long clientUid, ByteBuffer payload) {
        super();
        if (socketAddress == null) {
            throw new NullPointerException("socketAddress is null");
        }
        if (payload == null) {
            throw new NullPointerException("payload is null");
        }
        this.socketAddress = socketAddress;
        this.clientUid = clientUid;
        this.payload = payload;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleUdpIncomingPayload(this);
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public long getClientUid() {
        return clientUid;
    }

    public ByteBuffer getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(socetAddress=" + socketAddress + ", clientUid=" + clientUid +
                ", payload=" + payload + ")";
    }

    public interface Handler {
        void handleUdpIncomingPayload(UdpIncomingPayloadEvent event) throws InterruptedException;
    }
}