package com.omgameserver.engine.web;

import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.events.SecretKeyCreatedEvent;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Component
class WebService {
    static private final AtomicLong uidCounter = new AtomicLong();

    private final OmgsDispatcher dispatcher;

    WebService(OmgsDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    Authorization createAuthorization() throws NoSuchAlgorithmException, InterruptedException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = new SecureRandom();
        keyGenerator.init(128, secureRandom);
        long keyUid = uidCounter.incrementAndGet();
        SecretKey secretKey = keyGenerator.generateKey();
        dispatcher.getDispatcher().dispatch(new SecretKeyCreatedEvent(keyUid, secretKey));
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
