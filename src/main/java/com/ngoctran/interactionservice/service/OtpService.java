package com.ngoctran.interactionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;

/**
 * OTP Service - Handles OTP sending and verification
 * In production, this would integrate with SMS providers like Twilio, AWS SNS,
 * etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final RestTemplate restTemplate;
    private final SideEffectExecutor sideEffectExecutor;

    @Value("${otp.provider.url:http://localhost:8081/mock/otp}")
    private String otpProviderUrl;

    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    // In-memory store for demo - in production use Redis/database
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    /**
     * Send OTP to phone number
     */
    @CircuitBreaker(name = "otp-service")
    @RateLimiter(name = "otp-service")
    @Bulkhead(name = "external-api-bulkhead")
    public boolean sendOtp(UUID instanceId, String phoneNumber) {
        try {
            log.info("Sending OTP to phone: {}", phoneNumber);

            // Generate OTP deterministically (Using SideEffectExecutor)
            String otpCode = sideEffectExecutor.execute(instanceId, "otp-generation", String.class, this::generateOtp);

            // Store OTP with expiry
            String key = "phone:" + phoneNumber;
            otpStore.put(key, new OtpRecord(otpCode, System.currentTimeMillis()));

            // In production: call SMS provider API
            Map<String, Object> request = Map.of(
                    "phoneNumber", phoneNumber,
                    "message", "Your OTP code is: " + otpCode,
                    "expiryMinutes", otpExpiryMinutes);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    otpProviderUrl + "/send",
                    request,
                    String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("OTP sent to {}: {}", phoneNumber, success ? "SUCCESS" : "FAILED");

            return success;

        } catch (Exception ex) {
            log.error("Error sending OTP to {}: {}", phoneNumber, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Verify OTP code
     */
    public boolean verifyOtp(String phoneNumber, String otpCode) {
        try {
            log.info("Verifying OTP for phone: {}", phoneNumber);

            String key = "phone:" + phoneNumber;
            OtpRecord record = otpStore.get(key);

            if (record == null) {
                log.warn("No OTP found for phone: {}", phoneNumber);
                return false;
            }

            // Check expiry
            long ageMinutes = (System.currentTimeMillis() - record.timestamp) / (1000 * 60);
            if (ageMinutes > otpExpiryMinutes) {
                log.warn("OTP expired for phone: {}", phoneNumber);
                otpStore.remove(key); // Clean up
                return false;
            }

            // Check code
            boolean valid = record.otpCode.equals(otpCode.trim());
            if (valid) {
                log.info("OTP verified successfully for phone: {}", phoneNumber);
                otpStore.remove(key); // One-time use
            } else {
                log.warn("Invalid OTP for phone: {}", phoneNumber);
            }

            return valid;

        } catch (Exception ex) {
            log.error("Error verifying OTP for {}: {}", phoneNumber, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Verify OTP by instance ID (workflow integration)
     */
    public boolean verifyOtp(UUID instanceId, String otpCode) {
        // In production, you'd look up the phone number from the workflow context
        // For demo, we'll use a mock phone number
        String mockPhoneNumber = "+1234567890"; // Would be retrieved from workflow data
        return verifyOtp(mockPhoneNumber, otpCode);
    }

    /**
     * Resend OTP (with rate limiting)
     */
    @CircuitBreaker(name = "otp-service")
    @RateLimiter(name = "otp-service")
    public boolean resendOtp(UUID instanceId, String phoneNumber) {
        // Add rate limiting logic here
        return sendOtp(instanceId, phoneNumber);
    }

    private String generateOtp() {
        // Generate 6-digit OTP
        return String.format("%06d", (int) (Math.random() * 999999));
    }

    private static class OtpRecord {
        final String otpCode;
        final long timestamp;

        OtpRecord(String otpCode, long timestamp) {
            this.otpCode = otpCode;
            this.timestamp = timestamp;
        }
    }
}
