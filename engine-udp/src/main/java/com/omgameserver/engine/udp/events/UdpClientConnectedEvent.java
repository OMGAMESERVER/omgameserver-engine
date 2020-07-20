package com.omgameserver.engine.udp.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class UdpClientConnectedEvent extends Event<UdpClientConnectedEvent.Handler> {

    private final SocketAddress socketAddress;
    private final long clientUid;

    public UdpClientConnectedEvent(SocketAddress socketAddress, long clientUid) {
        super();
        if (socketAddress == null) {
            throw new NullPointerException("socketAddress is null");
        }
        this.socketAddress = socketAddress;
        this.clientUid = clientUid;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleUdpClientConnected(this);
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public long getClientUid() {
        return clientUid;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(socketAddress=" + socketAddress + ", clientUid=" + clientUid + ")";
    }

    public interface Handler {
        void handleUdpClientConnected(UdpClientConnectedEvent event) throws InterruptedException;
    }
}