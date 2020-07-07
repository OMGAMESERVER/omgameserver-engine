package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;
import org.luaj.vm2.LuaValue;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class IncomingLuaValueEvent extends Event<IncomingLuaValueEvent.Handler> {

    private final long clientUid;
    private final LuaValue luaValue;

    public IncomingLuaValueEvent(long clientUid, LuaValue luaValue) {
        super();
        if (luaValue == null) {
            throw new NullPointerException("luaValue is null");
        }
        this.clientUid = clientUid;
        this.luaValue = luaValue;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleIncomingLuaValue(this);
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
        void handleIncomingLuaValue(IncomingLuaValueEvent event) throws InterruptedException;
    }
}