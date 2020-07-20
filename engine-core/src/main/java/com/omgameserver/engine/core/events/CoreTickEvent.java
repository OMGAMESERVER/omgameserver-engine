package com.omgameserver.engine.core.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class CoreTickEvent extends Event<CoreTickEvent.Handler> {

    private final long number;
    private final long deltaTime;

    public CoreTickEvent(long number, long deltaTime) {
        super();
        this.number = number;
        this.deltaTime = deltaTime;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleCoreTick(this);
    }

    public long getNumber() {
        return number;
    }

    public long getDeltaTime() {
        return deltaTime;
    }

    public interface Handler {
        void handleCoreTick(CoreTickEvent event) throws InterruptedException;
    }
}
