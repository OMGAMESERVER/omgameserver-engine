package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaEventReceivedEvent extends Event<LuaEventReceivedEvent.Handler> {

    private final String eventId;
    private final String data;

    public LuaEventReceivedEvent(String eventId, String data) {
        super();
        this.eventId = eventId;
        this.data = data;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaEventReceivedEvent(this);
    }

    public String getEventId() {
        return eventId;
    }

    public String getData() {
        return data;
    }

    public interface Handler {
        void handleLuaEventReceivedEvent(LuaEventReceivedEvent event) throws InterruptedException;
    }
}