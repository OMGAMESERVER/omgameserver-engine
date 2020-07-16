package com.omgameserver.engine.lua.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaConnectedReceivedEvent extends Event<LuaConnectedReceivedEvent.Handler> {

    private final long clientUid;
    private final String clientType;

    public LuaConnectedReceivedEvent(long clientUid, String clientType) {
        super();
        this.clientUid = clientUid;
        this.clientType = clientType;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaConnectedReceivedEvent(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public String getClientType() {
        return clientType;
    }

    public interface Handler {
        void handleLuaConnectedReceivedEvent(LuaConnectedReceivedEvent event) throws InterruptedException;
    }
}