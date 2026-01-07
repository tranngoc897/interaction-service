package com.ngoctran.interactionservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker configuration to prevent cascading failures
 * Protects external services and prevents connection pool exhaustion
 */
@Slf4j
@Configuration
public class CircuitBreakerConfig {

        /**
         * Circuit breaker for eKYC service
         */
        @Bean
        public CircuitBreaker ekycCircuitBreaker() {
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                                .custom()
                                .failureRateThreshold(50) // Open circuit if >50% failures
                                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
                                .slidingWindowSize(10) // Check last 10 calls
                                .minimumNumberOfCalls(5) // Need at least 5 calls to calculate failure rate
                                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                                .recordExceptions(Exception.class) // Count all exceptions as failures
                                .build();

                CircuitBreaker circuitBreaker = CircuitBreaker.of("ekyc-service", config);

                // Event listeners for monitoring
                circuitBreaker.getEventPublisher()
                                .onStateTransition(event -> {
                                        log.info("eKYC CircuitBreaker state changed: {} -> {}",
                                                        event.getStateTransition().getFromState(),
                                                        event.getStateTransition().getToState());
                                })
                                .onFailureRateExceeded(event -> {
                                        log.warn("eKYC CircuitBreaker failure rate exceeded: {}%",
                                                        event.getFailureRate());
                                });

                return circuitBreaker;
        }

        /**
         * Circuit breaker for AML service
         */
        @Bean
        public CircuitBreaker amlCircuitBreaker() {
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                                .custom()
                                .failureRateThreshold(40) // More tolerant than eKYC
                                .waitDurationInOpenState(Duration.ofSeconds(60)) // Longer recovery time
                                .slidingWindowSize(20) // Larger window for AML
                                .minimumNumberOfCalls(10)
                                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                                .recordExceptions(Exception.class)
                                .build();

                CircuitBreaker circuitBreaker = CircuitBreaker.of("aml-service", config);

                circuitBreaker.getEventPublisher()
                                .onStateTransition(event -> {
                                        log.info("AML CircuitBreaker state changed: {} -> {}",
                                                        event.getStateTransition().getFromState(),
                                                        event.getStateTransition().getToState());
                                });

                return circuitBreaker;
        }

        /**
         * Circuit breaker for account creation service
         */
        @Bean
        public CircuitBreaker accountCircuitBreaker() {
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                                .custom()
                                .failureRateThreshold(30) // Most tolerant
                                .waitDurationInOpenState(Duration.ofSeconds(120)) // Long recovery for core banking
                                .slidingWindowSize(50) // Large window for critical service
                                .minimumNumberOfCalls(20)
                                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                                .recordExceptions(Exception.class)
                                .build();

                CircuitBreaker circuitBreaker = CircuitBreaker.of("account-service", config);

                circuitBreaker.getEventPublisher()
                                .onStateTransition(event -> {
                                        log.warn("Account CircuitBreaker state changed: {} -> {}",
                                                        event.getStateTransition().getFromState(),
                                                        event.getStateTransition().getToState());
                                });

                return circuitBreaker;
        }

        /**
         * Registry for managing all circuit breakers
         */
        @Bean
        public CircuitBreakerRegistry circuitBreakerRegistry() {
                return CircuitBreakerRegistry.ofDefaults();
        }
}
