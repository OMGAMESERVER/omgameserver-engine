package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class IncomingRawDataEvent extends Event<IncomingRawDataEvent.Handler> {

    private final SocketAddress socketAddress;
    private final ByteBuffer rawData;

    public IncomingRawDataEvent(SocketAddress socketAddress, ByteBuffer rawData) {
        super();
        if (socketAddress == null) {
            throw new NullPointerException("socketAddress is null");
        }
        if (rawData == null) {
            throw new NullPointerException("rawData is null");
        }
        this.socketAddress = socketAddress;
        this.rawData = rawData;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleIncomingRawData(this);
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public ByteBuffer getRawData() {
        return rawData;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(socketAddress=" + socketAddress + ", rawData=" + rawData + ")";
    }

    public interface Handler {
        void handleIncomingRawData(IncomingRawDataEvent event) throws InterruptedException;
    }
}