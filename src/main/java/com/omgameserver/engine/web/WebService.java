package com.omgameserver.engine.web;

import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.events.AuthorizationCreatedEvent;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

@Component
class WebService {
    static private final AtomicLong uidCounter = new AtomicLong();

    private final Dispatcher dispatcher;

    public WebService(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    Authorization createAuthorization() throws NoSuchAlgorithmException, InterruptedException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = new SecureRandom();
        keyGenerator.init(128, secureRandom);
        long keyUid = uidCounter.incrementAndGet();
        SecretKey secretKey = keyGenerator.generateKey();
        dispatcher.dispatch(new AuthorizationCreatedEvent(keyUid, secretKey));
        return new Authorization(keyUid, secretKey);
    }

    class Authorization {

        private final long keyUid;
        private final SecretKey secretKey;

        public Authorization(long keyUid, SecretKey secretKey) {
            this.keyUid = keyUid;
            this.secretKey = secretKey;
        }

        public long getKeyUid() {
            return keyUid;
        }

        public SecretKey getSecretKey() {
            return secretKey;
        }
    }
}
