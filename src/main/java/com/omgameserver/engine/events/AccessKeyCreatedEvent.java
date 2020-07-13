package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class AccessKeyCreatedEvent extends Event<AccessKeyCreatedEvent.Handler> {

    private final long accessKey;

    public AccessKeyCreatedEvent(long accessKey) {
        super();
        this.accessKey = accessKey;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleAccessKeyCreated(this);
    }

    public long getAccessKey() {
        return accessKey;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(accessKey=" + accessKey + ")";
    }

    public interface Handler {
        void handleAccessKeyCreated(AccessKeyCreatedEvent event) throws InterruptedException;
    }
}