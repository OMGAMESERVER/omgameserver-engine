package com.omgameserver.engine;

import com.crionuke.bolts.Dispatcher;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.UnknownHostException;

public class BaseServiceTest extends Assert {
    static private final Logger logger = LoggerFactory.getLogger(BaseServiceTest.class);

    protected final int POLL_TIMEOUT_MS = 1000;

    protected final String PROPERTY_HOST = "0.0.0.0";
    protected final int PROPERTY_PORT = 0;
    protected final int PROPERTY_THREAD_POOL_SIZE = 32;
    protected final int PROPERTY_QUEUE_SIZE = 128;
    protected final int PROPERTY_TICK_INTERVAL = 100;
    protected final int PROPERTY_DISCONNECT_INTERVAL = 5000;
    protected final int PROPERTY_PING_INTERVAL = 1000;
    protected final String PROPERTY_MAIN_SCRIPT = "main.lua";

    protected OmgsProperties properties;
    protected Dispatcher dispatcher;
    protected ThreadPoolTaskExecutor threadPoolTaskExecutor;

    protected void createComponents() throws UnknownHostException {
        properties = new OmgsProperties(PROPERTY_HOST, PROPERTY_PORT, PROPERTY_THREAD_POOL_SIZE, PROPERTY_QUEUE_SIZE,
                PROPERTY_TICK_INTERVAL, PROPERTY_DISCONNECT_INTERVAL, PROPERTY_PING_INTERVAL, PROPERTY_MAIN_SCRIPT);
        dispatcher = new Dispatcher();
        threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("test-");
        threadPoolTaskExecutor.setCorePoolSize(properties.getThreadPoolSize());
        threadPoolTaskExecutor.initialize();
        logger.info("Thread pool with size={} created", properties.getThreadPoolSize());
    }
}
