package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class LuaService extends Bolt {
    static private final Logger logger = LoggerFactory.getLogger(LuaService.class);

    private final OmgsProperties properties;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;
    private final ThreadPoolTaskExecutor workerExecutor;
    private final LuaWorker defaultWorker;

    LuaService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("lua", properties.getQueueSize());
        this.properties = properties;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
        workerExecutor = createWorkerExecutor();
        defaultWorker = new LuaWorker(properties, properties.getMainScript(), workerExecutor);
        defaultWorker.postConstruct();
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
    }

    private ThreadPoolTaskExecutor createWorkerExecutor() {
        ThreadPoolTaskExecutor workerExecutor = new ThreadPoolTaskExecutor();
        workerExecutor.setThreadNamePrefix("omgs-");
        workerExecutor.setCorePoolSize(properties.getThreadPoolSize());
        workerExecutor.initialize();
        logger.info("Worker pool with size={} created", properties.getThreadPoolSize());
        return workerExecutor;
    }
}
