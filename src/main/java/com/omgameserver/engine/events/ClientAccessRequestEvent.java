package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class ClientAccessRequestEvent extends Event<ClientAccessRequestEvent.Handler> {

    private final SocketAddress socketAddress;
    private final long clientUid;
    private final long accessKey;

    public ClientAccessRequestEvent(SocketAddress socketAddress, long clientUid, long accessKey) {
        super();
        if (socketAddress == null) {
            throw new NullPointerException("socketAddress is null");
        }
        this.socketAddress = socketAddress;
        this.clientUid = clientUid;
        this.accessKey = accessKey;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleClientAccessRequest(this);
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public long getClientUid() {
        return clientUid;
    }

    public long getAccessKey() {
        return accessKey;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(socketAddress=" + socketAddress + ", clientUid=" + clientUid +
                ", accessKey=" + accessKey + ")";
    }

    public interface Handler {
        void handleClientAccessRequest(ClientAccessRequestEvent event) throws InterruptedException;
    }
}