package com.ngoctran.interactionservice.workflow.activity;

import io.temporal.activity.ActivityInterface;

/**
 * Keycloak Account Creation Activity
 * Creates user account in Keycloak IAM system
 */
@ActivityInterface
public interface KeycloakAccountActivity {

    /**
     * Create user account in Keycloak
     */
    KeycloakResult createKeycloakAccount(String customerId, String cifNumber, String phoneNumber, String email);

    /**
     * Keycloak account creation result
     */
    class KeycloakResult {
        private boolean success;
        private String userId;
        private String username;
        private String temporaryPassword;
        private String errorMessage;
        private long createdAt;

        public KeycloakResult() {}

        public KeycloakResult(boolean success, String userId, String username,
                            String temporaryPassword, String errorMessage, long createdAt) {
            this.success = success;
            this.userId = userId;
            this.username = username;
            this.temporaryPassword = temporaryPassword;
            this.errorMessage = errorMessage;
            this.createdAt = createdAt;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getTemporaryPassword() { return temporaryPassword; }
        public void setTemporaryPassword(String temporaryPassword) { this.temporaryPassword = temporaryPassword; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }
}
