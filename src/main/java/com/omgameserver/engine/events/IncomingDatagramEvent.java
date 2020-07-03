package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class IncomingDatagramEvent extends Event<IncomingDatagramEvent.Handler> {

    private final SocketAddress sourceAddress;
    private final ByteBuffer byteBuffer;

    public IncomingDatagramEvent(SocketAddress sourceAddress, ByteBuffer byteBuffer) {
        super();
        if (sourceAddress == null) {
            throw new NullPointerException("sourceAddress is null");
        }
        if (byteBuffer == null) {
            throw new NullPointerException("byteBuffer is null");
        }
        this.sourceAddress = sourceAddress;
        this.byteBuffer = byteBuffer;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleIncomingDatagram(this);
    }

    public SocketAddress getSourceAddress() {
        return sourceAddress;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public interface Handler {
        void handleIncomingDatagram(IncomingDatagramEvent event) throws InterruptedException;
    }
}