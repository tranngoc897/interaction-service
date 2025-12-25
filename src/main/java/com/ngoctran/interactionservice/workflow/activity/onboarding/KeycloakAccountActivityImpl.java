package com.ngoctran.interactionservice.workflow.activity.onboarding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KeycloakAccountActivityImpl implements KeycloakAccountActivity {

    @Override
    public KeycloakResult createKeycloakAccount(String customerId, String cifNumber, String phoneNumber, String email) {
        log.info("Creating Keycloak account for customer: {}, CIF: {}, phone: {}", customerId, cifNumber, phoneNumber);

        try {
            Thread.sleep(1500 + (int)(Math.random() * 2000)); // 1.5-3.5 seconds

            // Generate user details
            String userId = "KC" + String.format("%012d", System.nanoTime() % 1000000000000L);
            String username = phoneNumber; // Use phone as username
            String temporaryPassword = "Temp" + String.format("%08d", System.nanoTime() % 100000000);

            boolean success = Math.random() > 0.02; // 98% success rate

            if (success) {
                log.info("Keycloak account created successfully: userId={}, username={}", userId, username);
                return new KeycloakResult(true, userId, username, temporaryPassword, null, System.currentTimeMillis());
            } else {
                String errorMessage = "Keycloak account creation failed";
                log.error("Keycloak account creation failed: {}", errorMessage);
                return new KeycloakResult(false, null, null, null, errorMessage, System.currentTimeMillis());
            }

        } catch (Exception e) {
            log.error("Keycloak account creation failed for customer: {}", customerId, e);
            return new KeycloakResult(false, null, null, null, "Keycloak error: " + e.getMessage(), System.currentTimeMillis());
        }
    }
}
