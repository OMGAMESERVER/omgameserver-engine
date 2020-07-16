package com.omgameserver.engine.udp.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class UdpIncomingDatagramEvent extends Event<UdpIncomingDatagramEvent.Handler> {

    private final SocketAddress socketAddress;
    private final ByteBuffer byteBuffer;

    public UdpIncomingDatagramEvent(SocketAddress socketAddress, ByteBuffer byteBuffer) {
        super();
        if (socketAddress == null) {
            throw new NullPointerException("socketAddress is null");
        }
        if (byteBuffer == null) {
            throw new NullPointerException("byteBuffer is null");
        }
        this.socketAddress = socketAddress;
        this.byteBuffer = byteBuffer;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleUdpIncomingDatagram(this);
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(socketAddress=" + socketAddress + ", byteBuffer=" + byteBuffer + ")";
    }

    public interface Handler {
        void handleUdpIncomingDatagram(UdpIncomingDatagramEvent event) throws InterruptedException;
    }
}