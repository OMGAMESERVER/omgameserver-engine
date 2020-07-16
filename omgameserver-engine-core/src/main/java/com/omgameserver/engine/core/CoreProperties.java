package com.omgameserver.engine.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Component
public class CoreProperties {

    private final int internalThreadPoolSize;
    private final int userThreadPoolSize;
    private final int tickInterval;

    public CoreProperties(@Value("${omgameserver.engine.core.internalThreadPoolSize:16}") int internalThreadPoolSize,
                          @Value("${omgameserver.engine.core.userThreadPoolSize:32}") int userThreadPoolSize,
                          @Value("${omgameserver.engine.core.tickInterval:100}") int tickInterval) {
        this.internalThreadPoolSize = internalThreadPoolSize;
        this.userThreadPoolSize = userThreadPoolSize;
        this.tickInterval = tickInterval;
    }

    public int getInternalThreadPoolSize() {
        return internalThreadPoolSize;
    }

    public int getUserThreadPoolSize() {
        return userThreadPoolSize;
    }

    public int getTickInterval() {
        return tickInterval;
    }
}
