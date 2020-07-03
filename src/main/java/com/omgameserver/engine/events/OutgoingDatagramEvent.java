package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class OutgoingDatagramEvent extends Event<OutgoingDatagramEvent.Handler> {

    private final SocketAddress targetAddress;
    private final ByteBuffer byteBuffer;

    public OutgoingDatagramEvent(SocketAddress targetAddress, ByteBuffer byteBuffer) {
        super();
        if (targetAddress == null) {
            throw new NullPointerException("targetAddress is null");
        }
        if (byteBuffer == null) {
            throw new NullPointerException("byteBuffer is null");
        }
        this.targetAddress = targetAddress;
        this.byteBuffer = byteBuffer;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleOutgoingDatagram(this);
    }

    public SocketAddress getTargetAddress() {
        return targetAddress;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public interface Handler {
        void handleOutgoingDatagram(OutgoingDatagramEvent event) throws InterruptedException;
    }
}