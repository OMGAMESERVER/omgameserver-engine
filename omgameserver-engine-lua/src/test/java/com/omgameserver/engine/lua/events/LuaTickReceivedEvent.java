package com.omgameserver.engine.lua.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaTickReceivedEvent extends Event<LuaTickReceivedEvent.Handler> {

    private final long number;
    private final long deltaTime;

    public LuaTickReceivedEvent(long number, long deltaTime) {
        super();
        this.number = number;
        this.deltaTime = deltaTime;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaTickReceivedEvent(this);
    }

    public long getNumber() {
        return number;
    }

    public long getDeltaTime() {
        return deltaTime;
    }

    public interface Handler {
        void handleLuaTickReceivedEvent(LuaTickReceivedEvent event) throws InterruptedException;
    }
}