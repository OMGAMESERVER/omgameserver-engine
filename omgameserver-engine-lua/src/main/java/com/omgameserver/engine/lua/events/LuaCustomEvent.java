package com.omgameserver.engine.lua.events;

import com.crionuke.bolts.Event;
import org.luaj.vm2.LuaValue;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaCustomEvent extends Event<LuaCustomEvent.Handler> {

    private final String id;
    private final LuaValue event;

    public LuaCustomEvent(String id, LuaValue event) {
        super();
        if (id == null) {
            throw new NullPointerException("id is null");
        }
        if (event == null) {
            throw new NullPointerException("events is null");
        }
        this.id = id;
        this.event = event;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaCustomEvent(this);
    }

    public String getId() {
        return id;
    }

    public LuaValue getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(id=" + id + ", events=" + event + ")";
    }

    public interface Handler {
        void handleLuaCustomEvent(LuaCustomEvent event) throws InterruptedException;
    }
}