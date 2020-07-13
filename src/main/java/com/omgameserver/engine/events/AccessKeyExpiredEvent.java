package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class AccessKeyExpiredEvent extends Event<AccessKeyExpiredEvent.Handler> {

    private final long accessKey;

    public AccessKeyExpiredEvent(long accessKey) {
        super();
        this.accessKey = accessKey;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleAccessKeyExpired(this);
    }

    public long getAccessKey() {
        return accessKey;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(accessKey=" + accessKey + ")";
    }

    public interface Handler {
        void handleAccessKeyExpired(AccessKeyExpiredEvent event) throws InterruptedException;
    }
}