package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;
import org.luaj.vm2.LuaValue;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class OutgoingLuaValueEvent extends Event<OutgoingLuaValueEvent.Handler> {

    private final long clientUid;
    private final LuaValue luaValue;
    private final boolean ephemeral;

    public OutgoingLuaValueEvent(long clientUid, LuaValue luaValue, boolean ephemeral) {
        super();
        if (luaValue == null) {
            throw new NullPointerException("luaValue is null");
        }
        this.clientUid = clientUid;
        this.luaValue = luaValue;
        this.ephemeral = ephemeral;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleOutgoingLuaValue(this);
    }

    public long getClientUid() {
        return clientUid;
    }

    public LuaValue getLuaValue() {
        return luaValue;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(clientUid=" + clientUid + ", luaValue=" + luaValue +
                ", ephemeral=" + ephemeral + ")";
    }

    public interface Handler {
        void handleOutgoingLuaValue(OutgoingLuaValueEvent event) throws InterruptedException;
    }
}