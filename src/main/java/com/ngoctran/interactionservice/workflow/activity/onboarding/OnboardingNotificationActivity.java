package com.ngoctran.interactionservice.workflow.activity.onboarding;

import io.temporal.activity.ActivityInterface;

/**
 * Onboarding Notification Activity
 * Handles all completion notifications (SMS, Push, Email)
 */
@ActivityInterface
public interface OnboardingNotificationActivity {

    /**
     * Send onboarding completion SMS
     */
    NotificationResult sendOnboardingSMSToCustomer(String customerId, String phoneNumber, String cifNumber);

    /**
     * Send onboarding completion push notification
     */
    NotificationResult sendOnboardingPushToCustomer(String customerId, String deviceToken, String cifNumber);

    /**
     * Send onboarding completion email
     */
    NotificationResult sendOnboardingEmailToCustomer(String customerId, String email, String cifNumber);

    /**
     * Notification result
     */
    class NotificationResult {
        private boolean success;
        private String messageId;
        private String channel;
        private String errorMessage;
        private long sentAt;

        public NotificationResult() {}

        public NotificationResult(boolean success, String messageId, String channel,
                                String errorMessage, long sentAt) {
            this.success = success;
            this.messageId = messageId;
            this.channel = channel;
            this.errorMessage = errorMessage;
            this.sentAt = sentAt;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public long getSentAt() { return sentAt; }
        public void setSentAt(long sentAt) { this.sentAt = sentAt; }
    }
}
