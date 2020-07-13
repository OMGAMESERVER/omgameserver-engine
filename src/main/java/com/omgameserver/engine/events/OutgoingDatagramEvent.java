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
    private final ByteBuffer datagram;

    public OutgoingDatagramEvent(SocketAddress targetAddress, ByteBuffer datagram) {
        super();
        if (targetAddress == null) {
            throw new NullPointerException("targetAddress is null");
        }
        if (datagram == null) {
            throw new NullPointerException("datagram is null");
        }
        this.targetAddress = targetAddress;
        this.datagram = datagram;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleOutgoingDatagram(this);
    }

    public SocketAddress getTargetAddress() {
        return targetAddress;
    }

    public ByteBuffer getDatagram() {
        return datagram;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(targetAddress=" + targetAddress + ", datagram=" + datagram + ")";
    }

    public interface Handler {
        void handleOutgoingDatagram(OutgoingDatagramEvent event) throws InterruptedException;
    }
}