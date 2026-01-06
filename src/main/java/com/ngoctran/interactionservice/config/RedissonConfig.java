package com.ngoctran.interactionservice.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson Configuration for distributed locking
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "redisson.enabled", havingValue = "true")
public class RedissonConfig {

    @Value("${redisson.config}")
    private String redissonConfigYaml;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        try {
            log.info("Initializing Redisson client for distributed locking");

            Config config = Config.fromYAML(redissonConfigYaml);

            RedissonClient client = Redisson.create(config);

            log.info("Redisson client initialized successfully");
            return client;

        } catch (Exception ex) {
            log.error("Failed to initialize Redisson client: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to initialize Redisson client", ex);
        }
    }
}
