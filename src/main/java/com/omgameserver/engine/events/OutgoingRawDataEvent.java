package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class OutgoingRawDataEvent extends Event<OutgoingRawDataEvent.Handler> {

    private final SocketAddress targetAddress;
    private final ByteBuffer rawData;

    public OutgoingRawDataEvent(SocketAddress targetAddress, ByteBuffer rawData) {
        super();
        if (targetAddress == null) {
            throw new NullPointerException("targetAddress is null");
        }
        if (rawData == null) {
            throw new NullPointerException("rawData is null");
        }
        this.targetAddress = targetAddress;
        this.rawData = rawData;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleOutgoingRawData(this);
    }

    public SocketAddress getTargetAddress() {
        return targetAddress;
    }

    public ByteBuffer getRawData() {
        return rawData;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(targetAddress=" + targetAddress + ", rawData=" + rawData + ")";
    }

    public interface Handler {
        void handleOutgoingRawData(OutgoingRawDataEvent event) throws InterruptedException;
    }
}