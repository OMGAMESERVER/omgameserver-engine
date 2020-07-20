package com.omgameserver.engine.lua.udp.events;

import com.crionuke.bolts.Event;
import org.luaj.vm2.LuaValue;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaUdpIncomingValueEvent extends Event<LuaUdpIncomingValueEvent.Handler> {

    private final long clientUid;
    private final LuaValue luaValue;

    public LuaUdpIncomingValueEvent(long clientUid, LuaValue luaValue) {
        super();
        if (luaValue == null) {
            throw new NullPointerException("luaValue is null");
        }
        this.clientUid = clientUid;
        this.luaValue = luaValue;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaUdpIncomingValue(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public LuaValue getLuaValue() {
        return luaValue;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(clientUid=" + clientUid + ", luaValue=" + luaValue + ")";
    }

    public interface Handler {
        void handleLuaUdpIncomingValue(LuaUdpIncomingValueEvent event) throws InterruptedException;
    }
}