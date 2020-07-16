package com.omgameserver.engine.lua.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaDataReceivedEvent extends Event<LuaDataReceivedEvent.Handler> {

    private final long clientUid;
    private final String data;

    public LuaDataReceivedEvent(long clientUid, String data) {
        super();
        this.clientUid = clientUid;
        this.data = data;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaDataReceived(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public String getData() {
        return data;
    }

    public interface Handler {
        void handleLuaDataReceived(LuaDataReceivedEvent event) throws InterruptedException;
    }
}