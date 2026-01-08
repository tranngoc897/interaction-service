package com.ngoctran.interactionservice.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate limiting configuration to prevent overwhelming external services
 * and protect against connection pool exhaustion
 */
@Slf4j
@Configuration
public class RateLimitConfig {

        /**
         * Rate limiter for eKYC service calls
         * Prevents overwhelming eKYC provider
         */
        @Bean
        public RateLimiter ekycRateLimiter() {
                RateLimiterConfig config = RateLimiterConfig.custom()
                                .limitForPeriod(10) // Max 10 requests
                                .limitRefreshPeriod(Duration.ofSeconds(1)) // Per second
                                .timeoutDuration(Duration.ofMillis(100)) // Wait up to 100ms
                                .build();

                RateLimiter rateLimiter = RateLimiter.of("ekyc-rate-limiter", config);

                // Event listeners for monitoring
                rateLimiter.getEventPublisher()
                                .onSuccess(event -> log.debug("eKYC rate limit success"))
                                .onFailure(event -> log.warn("eKYC rate limit exceeded - blocking request"));

                return rateLimiter;
        }

        /**
         * Rate limiter for AML service calls
         */
        @Bean
        public RateLimiter amlRateLimiter() {
                RateLimiterConfig config = RateLimiterConfig.custom()
                                .limitForPeriod(5) // More conservative for AML
                                .limitRefreshPeriod(Duration.ofSeconds(1))
                                .timeoutDuration(Duration.ofMillis(200)) // Longer timeout
                                .build();

                RateLimiter rateLimiter = RateLimiter.of("aml-rate-limiter", config);

                rateLimiter.getEventPublisher()
                                .onFailure(event -> log.warn("AML rate limit exceeded - blocking request"));

                return rateLimiter;
        }

        /**
         * Rate limiter for account creation service
         */
        @Bean
        public RateLimiter accountRateLimiter() {
                RateLimiterConfig config = RateLimiterConfig.custom()
                                .limitForPeriod(2) // Very conservative for core banking
                                .limitRefreshPeriod(Duration.ofSeconds(1))
                                .timeoutDuration(Duration.ofMillis(500)) // Longer timeout
                                .build();

                RateLimiter rateLimiter = RateLimiter.of("account-rate-limiter", config);

                rateLimiter.getEventPublisher()
                                .onFailure(event -> log.error("Account creation rate limit exceeded - critical!"));

                return rateLimiter;
        }

        /**
         * Rate limiter for workflow actions (user requests)
         * Prevents users from overwhelming the system
         */
        @Bean
        public RateLimiter userActionRateLimiter() {
                RateLimiterConfig config = RateLimiterConfig.custom()
                                .limitForPeriod(20) // Max 20 actions per user
                                .limitRefreshPeriod(Duration.ofMinutes(1)) // Per minute
                                .timeoutDuration(Duration.ofMillis(50))
                                .build();

                RateLimiter rateLimiter = RateLimiter.of("user-action-rate-limiter", config);

                rateLimiter.getEventPublisher()
                                .onFailure(event -> log.warn("User action rate limit exceeded"));

                return rateLimiter;
        }

        /**
         * Rate Limiter for OTP service
         * Limits the rate of OTP requests to save costs and prevent abuse
         */
        @Bean
        public RateLimiter otpRateLimiter() {
                RateLimiterConfig config = RateLimiterConfig.custom()
                                .limitRefreshPeriod(Duration.ofMinutes(1))
                                .limitForPeriod(10) // Max 10 OTPs per minute global (or per user if key resolver used)
                                .timeoutDuration(Duration.ofMillis(500))
                                .build();

                RateLimiter rateLimiter = RateLimiter.of("otp-service", config);

                rateLimiter.getEventPublisher()
                                .onSuccess(event -> log.debug("OTP RateLimiter permitted call"))
                                .onFailure(event -> log.warn("OTP RateLimiter rejected call"));

                return rateLimiter;
        }

        /**
         * Rate Limiter for External APIs generally
         */
        @Bean
        public RateLimiter externalApiRateLimiter() {
                RateLimiterConfig config = RateLimiterConfig.custom()
                                .limitRefreshPeriod(Duration.ofSeconds(1))
                                .limitForPeriod(50) // 50 RPS
                                .timeoutDuration(Duration.ofMillis(100))
                                .build();

                return RateLimiter.of("external-api", config);
        }

        /**
         * Registry for managing all rate limiters
         */
        @Bean
        public RateLimiterRegistry rateLimiterRegistry() {
                return RateLimiterRegistry.ofDefaults();
        }
}
