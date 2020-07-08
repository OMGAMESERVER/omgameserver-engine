package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class SecretKeyExpiredEvent extends Event<SecretKeyExpiredEvent.Handler> {

    private final long keyUid;

    public SecretKeyExpiredEvent(long keyUid) {
        super();
        this.keyUid = keyUid;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleSecretKeyExpired(this);
    }

    public long getKeyUid() {
        return keyUid;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(keyUid=" + keyUid + ")";
    }

    public interface Handler {
        void handleSecretKeyExpired(SecretKeyExpiredEvent event) throws InterruptedException;
    }
}