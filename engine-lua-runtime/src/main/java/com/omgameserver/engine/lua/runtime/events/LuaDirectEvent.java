package com.omgameserver.engine.lua.runtime.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaDirectEvent extends Event<LuaDirectEvent.Handler> {

    private final long uid;
    private final Event event;

    public LuaDirectEvent(long uid, Event event) {
        super();
        if (event == null) {
            throw new NullPointerException("events is null");
        }
        this.uid = uid;
        this.event = event;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaDirectEvent(this);
    }

    public long getUid() {
        return uid;
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(uid=" + uid + ", event=" + event + ")";
    }

    public interface Handler {
        void handleLuaDirectEvent(LuaDirectEvent event) throws InterruptedException;
    }
}