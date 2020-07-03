package com.omgameserver.engine;

import com.crionuke.bolts.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Configuration
class OmgsConfiguration {
    static private final Logger logger = LoggerFactory.getLogger(OmgsConfiguration.class);

    private final OmgsProperties properties;

    OmgsConfiguration(OmgsProperties properties) {
        this.properties = properties;
    }

    @Bean
    Dispatcher getDispatcher() {
        return new Dispatcher();
    }

    @Bean
    ThreadPoolTaskExecutor getThreadPoolExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("omgs-");
        threadPoolTaskExecutor.setCorePoolSize(properties.getThreadPoolSize());
        threadPoolTaskExecutor.initialize();
        logger.info("Thread pool with size={} created", properties.getThreadPoolSize());
        return threadPoolTaskExecutor;
    }
}
