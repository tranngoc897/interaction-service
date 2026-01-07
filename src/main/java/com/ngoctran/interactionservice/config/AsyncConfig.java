package com.ngoctran.interactionservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async processing to prevent OOM and connection pool exhaustion
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Thread pool for async operations (non-blocking external calls)
     * Prevents thread starvation and OOM
     */
    @Bean("asyncExecutor")
    public ThreadPoolTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum threads to keep alive
        executor.setCorePoolSize(4);

        // Max pool size - maximum threads during peak load
        executor.setMaxPoolSize(16);

        // Queue capacity - buffer requests when all threads are busy
        executor.setQueueCapacity(100);

        // Thread naming for debugging
        executor.setThreadNamePrefix("async-");

        // Keep alive time for idle threads
        executor.setKeepAliveSeconds(60);

        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // Reject policy - prevent unbounded queue growth
        executor.setRejectedExecutionHandler(
            (runnable, threadPoolExecutor) -> {
                log.warn("Async task rejected - queue full. Consider increasing pool size or queue capacity");
                // Could send alert here
            }
        );

        executor.initialize();
        log.info("Initialized async executor with core: {}, max: {}, queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Exception handler for async methods
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, java.lang.reflect.Method method, Object... params) {
                log.error("Uncaught async exception in method: {} with params: {}",
                        method.getName(), params, ex);
                // Could send alert or store in incident table
            }
        };
    }

    /**
     * Default executor for @Async without qualifier
     */
    @Override
    public Executor getAsyncExecutor() {
        return asyncExecutor();
    }
}
