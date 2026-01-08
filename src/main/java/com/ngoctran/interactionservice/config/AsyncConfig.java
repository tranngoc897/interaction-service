package com.ngoctran.interactionservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Default executor for general async tasks
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // Minimum threads
        executor.setMaxPoolSize(50); // Maximum threads
        executor.setQueueCapacity(100); // Queue before growing pool
        executor.setThreadNamePrefix("Async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated executor for critical workflow background tasks (Outbox, Scheduler)
     * This ensures background processing isn't starved by general async tasks.
     */
    @Bean(name = "workflowInternalExecutor")
    public Executor workflowInternalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("WorkflowInternal-");
        // CallerRuns ensures that if the pool is full, the scheduler thread itself
        // executes the task
        // slowing down the intake instead of dropping tasks.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
