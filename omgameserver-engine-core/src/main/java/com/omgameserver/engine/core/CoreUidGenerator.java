package com.omgameserver.engine.core;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Component
public class CoreUidGenerator {

    private final AtomicLong uidCounter;

    public CoreUidGenerator() {
        uidCounter = new AtomicLong();
    }

    public long getNext() {
        return uidCounter.incrementAndGet();
    }
}
