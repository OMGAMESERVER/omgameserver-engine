package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class ClientDisconnectedEvent extends Event<ClientDisconnectedEvent.Handler> {

    private final SocketAddress socketAddress;
    private final long clientUid;

    public ClientDisconnectedEvent(SocketAddress socketAddress, long clientUid) {
        super();
        if (socketAddress == null) {
            throw new NullPointerException("socketAddress is null");
        }
        this.socketAddress = socketAddress;
        this.clientUid = clientUid;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleClientDisconnected(this);
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public long getClientUid() {
        return clientUid;
    }

    public interface Handler {
        void handleClientDisconnected(ClientDisconnectedEvent event) throws InterruptedException;
    }
}