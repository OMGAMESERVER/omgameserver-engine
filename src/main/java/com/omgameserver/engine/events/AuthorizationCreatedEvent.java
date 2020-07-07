package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import javax.crypto.SecretKey;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class AuthorizationCreatedEvent extends Event<AuthorizationCreatedEvent.Handler> {

    private final long keyUid;
    private final SecretKey secretKey;

    public AuthorizationCreatedEvent(long keyUid, SecretKey secretKey) {
        super();
        this.keyUid = keyUid;
        this.secretKey = secretKey;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleAuthorizationCreated(this);
    }

    public long getKeyUid() {
        return keyUid;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(keyUid=" + keyUid + ", secretKey=" + secretKey + ")";
    }

    public interface Handler {
        void handleAuthorizationCreated(AuthorizationCreatedEvent event) throws InterruptedException;
    }
}