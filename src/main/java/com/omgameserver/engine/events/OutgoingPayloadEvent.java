package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class OutgoingPayloadEvent extends Event<OutgoingPayloadEvent.Handler> {

    private final long clientUid;
    private final ByteBuffer payload;
    private final boolean ephemeral;

    public OutgoingPayloadEvent(long clientUid, ByteBuffer payload, boolean ephemeral) {
        super();
        if (payload == null) {
            throw new NullPointerException("payload is null");
        }
        this.clientUid = clientUid;
        this.payload = payload;
        this.ephemeral = ephemeral;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleOutgoingPayload(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public ByteBuffer getPayload() {
        return payload;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public interface Handler {
        void handleOutgoingPayload(OutgoingPayloadEvent event) throws InterruptedException;
    }
}