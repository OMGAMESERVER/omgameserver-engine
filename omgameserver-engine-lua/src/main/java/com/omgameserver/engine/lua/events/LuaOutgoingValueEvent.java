package com.omgameserver.engine.lua.events;

import com.crionuke.bolts.Event;
import org.luaj.vm2.LuaValue;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaOutgoingValueEvent extends Event<LuaOutgoingValueEvent.Handler> {

    private final long clientUid;
    private final LuaValue luaValue;
    private final boolean reliable;

    public LuaOutgoingValueEvent(long clientUid, LuaValue luaValue, boolean reliable) {
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
        handler.handleLuaOutgoingValue(this);
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
        void handleLuaOutgoingValue(LuaOutgoingValueEvent event) throws InterruptedException;
    }
}