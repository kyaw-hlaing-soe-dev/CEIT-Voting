package com.KTU.KTUVotingapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for non-blocking operations.
 * Optimized for high concurrency scenarios.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool executor for async operations.
     * Optimized for 1500+ concurrent users.
     */
    @Bean(name = "asyncVotingExecutor")
    public Executor asyncVotingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-voting-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}

