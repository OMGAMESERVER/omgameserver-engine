package com.omgameserver.engine.udp.events;

import com.crionuke.bolts.Event;

import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class UdpOutgoingPayloadEvent extends Event<UdpOutgoingPayloadEvent.Handler> {

    private final long clientUid;
    private final ByteBuffer payload;
    private final boolean reliable;

    public UdpOutgoingPayloadEvent(long clientUid, ByteBuffer payload, boolean reliable) {
        super();
        if (payload == null) {
            throw new NullPointerException("payload is null");
        }
        this.clientUid = clientUid;
        this.payload = payload;
        this.reliable = reliable;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleUdpOutgoingPayload(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public ByteBuffer getPayload() {
        return payload;
    }

    public boolean isReliable() {
        return reliable;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(clientUid=" + clientUid + ", payload=" + payload +
                ", reliable=" + reliable + ")";
    }

    public interface Handler {
        void handleUdpOutgoingPayload(UdpOutgoingPayloadEvent event) throws InterruptedException;
    }
}