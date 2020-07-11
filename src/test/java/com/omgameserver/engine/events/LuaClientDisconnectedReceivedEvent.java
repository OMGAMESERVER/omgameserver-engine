package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaClientDisconnectedReceivedEvent extends Event<LuaClientDisconnectedReceivedEvent.Handler> {

    private final long clientUid;

    public LuaClientDisconnectedReceivedEvent(long clientUid) {
        super();
        this.clientUid = clientUid;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaClientDisconnectedReceivedEvent(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public interface Handler {
        void handleLuaClientDisconnectedReceivedEvent(LuaClientDisconnectedReceivedEvent event) throws InterruptedException;
    }
}