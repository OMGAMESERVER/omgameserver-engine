package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class IncomingPayloadEvent extends Event<IncomingPayloadEvent.Handler> {

    private final long clientUid;
    private final ByteBuffer payload;

    public IncomingPayloadEvent(long clientUid, ByteBuffer payload) {
        super();
        if (payload == null) {
            throw new NullPointerException("payload is null");
        }
        this.clientUid = clientUid;
        this.payload = payload;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleIncomingPayload(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public ByteBuffer getPayload() {
        return payload;
    }

    public interface Handler {
        void handleIncomingPayload(IncomingPayloadEvent event) throws InterruptedException;
    }
}