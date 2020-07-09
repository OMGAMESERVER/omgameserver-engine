package com.omgameserver.engine.events;

import com.crionuke.bolts.Event;

import javax.crypto.SecretKey;
import java.net.SocketAddress;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class SecretKeyAssignedEvent extends Event<SecretKeyAssignedEvent.Handler> {

    private final long keyUid;
    private final SecretKey secretKey;
    private final SocketAddress socketAddress;

    public SecretKeyAssignedEvent(long keyUid, SecretKey secretKey, SocketAddress socketAddress) {
        super();
        if (secretKey == null) {
            throw new NullPointerException("secretKey is null");
        }
        if (socketAddress == null) {
            throw new NullPointerException("socketAddress is null");
        }
        this.keyUid = keyUid;
        this.secretKey = secretKey;
        this.socketAddress = socketAddress;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleSecretKeyAssigned(this);
    }

    public long getKeyUid() {
        return keyUid;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(keyUid=" + keyUid + ", secretKey=" + secretKey +
                ", socketAddress=" + socketAddress + ")";
    }

    public interface Handler {
        void handleSecretKeyAssigned(SecretKeyAssignedEvent event) throws InterruptedException;
    }
}