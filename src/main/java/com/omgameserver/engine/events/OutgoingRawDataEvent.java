package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class OutgoingRawDataEvent extends Event<OutgoingRawDataEvent.Handler> {

    private final long clientUid;
    private final ByteBuffer rawData;
    private final boolean ephemeral;

    public OutgoingRawDataEvent(long clientUid, ByteBuffer rawData, boolean ephemeral) {
        super();
        if (rawData == null) {
            throw new NullPointerException("rawData is null");
        }
        this.clientUid = clientUid;
        this.rawData = rawData;
        this.ephemeral = ephemeral;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleOutgoingRawData(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public ByteBuffer getRawData() {
        return rawData;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(clientUid=" + clientUid + ", rawData=" + rawData +
                ", ephemeral=" + ephemeral + ")";
    }

    public interface Handler {
        void handleOutgoingRawData(OutgoingRawDataEvent event) throws InterruptedException;
    }
}