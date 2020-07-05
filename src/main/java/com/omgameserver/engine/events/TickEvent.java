package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class TickEvent extends Event<TickEvent.Handler> {

    private final long number;
    private final long deltaTime;

    public TickEvent(long number, long deltaTime) {
        super();
        this.number = number;
        this.deltaTime = deltaTime;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleTick(this);
    }

    public long getNumber() {
        return number;
    }

    public long getDeltaTime() {
        return deltaTime;
    }

    public interface Handler {
        void handleTick(TickEvent event) throws InterruptedException;
    }
}
