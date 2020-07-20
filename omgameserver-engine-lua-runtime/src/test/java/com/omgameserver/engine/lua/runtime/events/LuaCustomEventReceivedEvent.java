package com.omgameserver.engine.lua.runtime.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaCustomEventReceivedEvent extends Event<LuaCustomEventReceivedEvent.Handler> {

    private final String eventId;
    private final String data;

    public LuaCustomEventReceivedEvent(String eventId, String data) {
        super();
        this.eventId = eventId;
        this.data = data;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaCustomEventReceivedEvent(this);
    }

    public String getEventId() {
        return eventId;
    }

    public String getData() {
        return data;
    }

    public interface Handler {
        void handleLuaCustomEventReceivedEvent(LuaCustomEventReceivedEvent event) throws InterruptedException;
    }
}