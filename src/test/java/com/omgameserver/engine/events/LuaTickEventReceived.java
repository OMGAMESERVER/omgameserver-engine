package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class LuaTickEventReceived extends Event<LuaTickEventReceived.Handler> {

    private final long number;
    private final long deltaTime;

    public LuaTickEventReceived(long number, long deltaTime) {
        super();
        this.number = number;
        this.deltaTime = deltaTime;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleLuaTickEventReceived(this);
    }

    public long getNumber() {
        return number;
    }

    public long getDeltaTime() {
        return deltaTime;
    }

    public interface Handler {
        void handleLuaTickEventReceived(LuaTickEventReceived event) throws InterruptedException;
    }
}