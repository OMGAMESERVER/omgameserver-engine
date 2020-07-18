package com.omgameserver.engine.lua.events;

import com.crionuke.bolts.Event;
import org.luaj.vm2.LuaValue;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaCustomEvent extends Event<LuaCustomEvent.Handler> {

    private final String eventId;
    private final LuaValue luaEvent;

    public LuaCustomEvent(String eventId, LuaValue luaEvent) {
        super();
        if (eventId == null) {
            throw new NullPointerException("eventId is null");
        }
        if (luaEvent == null) {
            throw new NullPointerException("luaEvent is null");
        }
        this.eventId = eventId;
        this.luaEvent = luaEvent;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaCustomEvent(this);
    }

    public String getEventId() {
        return eventId;
    }

    public LuaValue getLuaEvent() {
        return luaEvent;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(eventId=" + eventId + ", luaEvent=" + luaEvent + ")";
    }

    public interface Handler {
        void handleLuaCustomEvent(LuaCustomEvent event) throws InterruptedException;
    }
}