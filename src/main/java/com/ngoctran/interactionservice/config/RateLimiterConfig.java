package com.ngoctran.interactionservice.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class RateLimiterConfig {

    /**
     * Rate Limiter for OTP service
     * Limits the rate of OTP requests to save costs and prevent abuse
     */
    @Bean
    public RateLimiter otpRateLimiter() {
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = io.github.resilience4j.ratelimiter.RateLimiterConfig
                .custom()
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
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = io.github.resilience4j.ratelimiter.RateLimiterConfig
                .custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(50) // 50 RPS
                .timeoutDuration(Duration.ofMillis(100))
                .build();

        return RateLimiter.of("external-api", config);
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }
}
