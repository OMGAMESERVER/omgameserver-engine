package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class DisconnectClientRequestEvent extends Event<DisconnectClientRequestEvent.Handler> {

    private final long clientUid;

    public DisconnectClientRequestEvent(long clientUid) {
        super();
        this.clientUid = clientUid;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleDisconnectClientRequest(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public interface Handler {
        void handleDisconnectClientRequest(DisconnectClientRequestEvent event) throws InterruptedException;
    }
}