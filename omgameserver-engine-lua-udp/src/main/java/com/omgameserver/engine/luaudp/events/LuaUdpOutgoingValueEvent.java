package com.omgameserver.engine.luaudp.events;

import com.crionuke.bolts.Event;
import org.luaj.vm2.LuaValue;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaUdpOutgoingValueEvent extends Event<LuaUdpOutgoingValueEvent.Handler> {

    private final long clientUid;
    private final LuaValue luaValue;
    private final boolean reliable;

    public LuaUdpOutgoingValueEvent(long clientUid, LuaValue luaValue, boolean reliable) {
        super();
        if (luaValue == null) {
            throw new NullPointerException("luaValue is null");
        }
        this.clientUid = clientUid;
        this.luaValue = luaValue;
        this.reliable = reliable;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaUdpOutgoingValue(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public LuaValue getLuaValue() {
        return luaValue;
    }

    public boolean isReliable() {
        return reliable;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(clientUid=" + clientUid + ", luaValue=" + luaValue +
                ", reliable=" + reliable + ")";
    }

    public interface Handler {
        void handleLuaUdpOutgoingValue(LuaUdpOutgoingValueEvent event) throws InterruptedException;
    }
}