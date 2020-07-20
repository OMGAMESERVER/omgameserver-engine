package com.omgameserver.engine.lua.runtime.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaDisconnectedReceivedEvent extends Event<LuaDisconnectedReceivedEvent.Handler> {

    private final long clientUid;
    private final String clientType;

    public LuaDisconnectedReceivedEvent(long clientUid, String clientType) {
        super();
        this.clientUid = clientUid;
        this.clientType = clientType;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaDisconnectedReceivedEvent(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public String getClientType() {
        return clientType;
    }

    public interface Handler {
        void handleLuaDisconnectedReceivedEvent(LuaDisconnectedReceivedEvent event) throws InterruptedException;
    }
}