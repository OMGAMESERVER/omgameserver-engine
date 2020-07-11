package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaClientConnectedReceivedEvent extends Event<LuaClientConnectedReceivedEvent.Handler> {

    private final long clientUid;

    public LuaClientConnectedReceivedEvent(long clientUid) {
        super();
        this.clientUid = clientUid;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaClientConnectedReceivedEvent(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public interface Handler {
        void handleLuaClientConnectedReceivedEvent(LuaClientConnectedReceivedEvent event) throws InterruptedException;
    }
}