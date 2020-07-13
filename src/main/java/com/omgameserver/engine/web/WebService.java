package com.omgameserver.engine.web;

import com.omgameserver.engine.OmgsDispatcher;
import com.omgameserver.engine.events.AccessKeyCreatedEvent;
import org.springframework.stereotype.Component;

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
    private final SecureRandom secureRandom;

    WebService(OmgsDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        secureRandom = new SecureRandom();
    }

    Access createAccess() throws InterruptedException {
        long accessKey = secureRandom.nextLong();
        dispatcher.getDispatcher().dispatch(new AccessKeyCreatedEvent(accessKey));
        return new Access(accessKey);
    }

    class Access {

        private final long accessKey;

        public Access(long accessKey) {
            this.accessKey = accessKey;
        }

        public long getAccessKey() {
            return accessKey;
        }
    }
}
