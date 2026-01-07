package com.ngoctran.interactionservice.config;

import io.github.resilience4j.bulkhead.Bulkhead;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bulkhead configuration to isolate different types of operations
 * Prevents one failing service from affecting others
 */
@Slf4j
@Configuration
public class BulkheadConfig {

        /**
         * Bulkhead for external API calls
         * Limits concurrent calls to prevent overwhelming external services
         */
        @Bean
        public Bulkhead externalApiBulkhead() {
                io.github.resilience4j.bulkhead.BulkheadConfig config = io.github.resilience4j.bulkhead.BulkheadConfig
                                .custom()
                                .maxConcurrentCalls(5) // Max 5 concurrent external calls
                                .maxWaitDuration(java.time.Duration.ofMillis(500)) // Wait up to 500ms
                                .build();

                Bulkhead bulkhead = Bulkhead.of("external-api-bulkhead", config);

                bulkhead.getEventPublisher()
                                .onCallPermitted(event -> log.debug("External API call permitted"))
                                .onCallRejected(event -> log.warn("External API call rejected - bulkhead full"))
                                .onCallFinished(event -> log.debug("External API call finished"));

                return bulkhead;
        }

        /**
         * Bulkhead for database operations
         * Protects database connection pool
         */
        @Bean
        public Bulkhead databaseBulkhead() {
                io.github.resilience4j.bulkhead.BulkheadConfig config = io.github.resilience4j.bulkhead.BulkheadConfig
                                .custom()
                                .maxConcurrentCalls(10) // Max 10 concurrent DB operations
                                .maxWaitDuration(java.time.Duration.ofMillis(200))
                                .build();

                Bulkhead bulkhead = Bulkhead.of("database-bulkhead", config);

                bulkhead.getEventPublisher()
                                .onCallRejected(event -> log
                                                .error("Database call rejected - connection pool may be exhausted"));

                return bulkhead;
        }

        /**
         * Bulkhead for Kafka operations
         * Prevents overwhelming message broker
         */
        @Bean
        public Bulkhead kafkaBulkhead() {
                io.github.resilience4j.bulkhead.BulkheadConfig config = io.github.resilience4j.bulkhead.BulkheadConfig
                                .custom()
                                .maxConcurrentCalls(20) // Higher limit for async messaging
                                .maxWaitDuration(java.time.Duration.ofMillis(100))
                                .build();

                Bulkhead bulkhead = Bulkhead.of("kafka-bulkhead", config);

                bulkhead.getEventPublisher()
                                .onCallRejected(event -> log.warn("Kafka call rejected - broker may be overwhelmed"));

                return bulkhead;
        }

        /**
         * Registry for managing all bulkheads
         */
        @Bean
        public BulkheadRegistry bulkheadRegistry() {
                return BulkheadRegistry.ofDefaults();
        }
}
