package com.omgameserver.engine;

import com.crionuke.bolts.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Component
public class EngineExecutors {
    static private final Logger logger = LoggerFactory.getLogger(EngineExecutors.class);

    private final ThreadPoolTaskExecutor internalExecutor;
    private final ThreadPoolTaskExecutor userExecutor;

    EngineExecutors(EngineProperties properties) {
        internalExecutor = createExecutor(properties.getInternalThreadPoolSize());
        userExecutor = createExecutor(properties.getUserThreadPoolSize());
    }

    public void executeInInternalPool(Worker worker) {
        internalExecutor.execute(worker);
        if (logger.isDebugEnabled()) {
            logger.debug("{} executed in internal thread pool, used {}/{} threads",
                    worker, internalExecutor.getActiveCount(), internalExecutor.getCorePoolSize());
        }
    }

    public void executeInUserPool(Worker worker) {
        userExecutor.execute(worker);
        if (logger.isDebugEnabled()) {
            logger.debug("{} executed in user thread pool, used {}/{} threads",
                    worker, userExecutor.getActiveCount(), userExecutor.getCorePoolSize());
        }
    }

    private ThreadPoolTaskExecutor createExecutor(int size) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(size);
        executor.initialize();
        logger.info("Executor with size={} created", size);
        return executor;
    }
}
