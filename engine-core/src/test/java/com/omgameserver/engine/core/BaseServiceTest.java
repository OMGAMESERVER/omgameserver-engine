package com.omgameserver.engine.core;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class BaseServiceTest extends Assertions {
    static private final Logger logger = LoggerFactory.getLogger(BaseServiceTest.class);

    protected final int POLL_TIMEOUT_MS = 1000;
    protected final int QUEUE_SIZE = 32;

    protected final int CORE_INTERNAL_THREAD_POOL_SIZE = 16;
    protected final int CORE_USER_THREAD_POOL_SIZE = 32;
    protected final int CORE_TICK_INTERVAL = 100;

    protected CoreProperties coreProperties;
    protected CoreDispatcher coreDispatcher;
    protected CoreExecutors coreExecutors;

    protected void createComponents() throws UnknownHostException {
        coreProperties = new CoreProperties(CORE_INTERNAL_THREAD_POOL_SIZE, CORE_USER_THREAD_POOL_SIZE,
                CORE_TICK_INTERVAL);
        coreDispatcher = new CoreDispatcher();
        coreExecutors = new CoreExecutors(coreProperties);
    }
}
