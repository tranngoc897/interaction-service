package com.ngoctran.interactionservice.workflow.activity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OnboardingNotificationActivityImpl implements OnboardingNotificationActivity {

    @Override
    public NotificationResult sendOnboardingSMSToCustomer(String customerId, String phoneNumber, String cifNumber) {
        log.info("Sending onboarding completion SMS to customer: {}, phone: {}", customerId, phoneNumber);

        try {
            Thread.sleep(500 + (int)(Math.random() * 1000)); // 0.5-1.5 seconds

            String messageId = "SMS" + String.format("%012d", System.nanoTime() % 1000000000000L);
            boolean success = Math.random() > 0.02; // 98% success rate

            if (success) {
                log.info("Onboarding SMS sent successfully: messageId={}", messageId);
                return new NotificationResult(true, messageId, "SMS", null, System.currentTimeMillis());
            } else {
                String errorMessage = "SMS service temporarily unavailable";
                log.error("SMS sending failed: {}", errorMessage);
                return new NotificationResult(false, null, "SMS", errorMessage, System.currentTimeMillis());
            }

        } catch (Exception e) {
            log.error("SMS sending failed for customer: {}", customerId, e);
            return new NotificationResult(false, null, "SMS", "SMS error: " + e.getMessage(), System.currentTimeMillis());
        }
    }

    @Override
    public NotificationResult sendOnboardingPushToCustomer(String customerId, String deviceToken, String cifNumber) {
        log.info("Sending onboarding completion push notification to customer: {}, device: {}", customerId, deviceToken);

        try {
            Thread.sleep(300 + (int)(Math.random() * 700)); // 0.3-1 second

            String messageId = "PUSH" + String.format("%012d", System.nanoTime() % 1000000000000L);
            boolean success = Math.random() > 0.03; // 97% success rate

            if (success) {
                log.info("Onboarding push notification sent successfully: messageId={}", messageId);
                return new NotificationResult(true, messageId, "PUSH", null, System.currentTimeMillis());
            } else {
                String errorMessage = "Push notification service temporarily unavailable";
                log.error("Push notification sending failed: {}", errorMessage);
                return new NotificationResult(false, null, "PUSH", errorMessage, System.currentTimeMillis());
            }

        } catch (Exception e) {
            log.error("Push notification sending failed for customer: {}", customerId, e);
            return new NotificationResult(false, null, "PUSH", "Push error: " + e.getMessage(), System.currentTimeMillis());
        }
    }

    @Override
    public NotificationResult sendOnboardingEmailToCustomer(String customerId, String email, String cifNumber) {
        log.info("Sending onboarding completion email to customer: {}, email: {}", customerId, email);

        try {
            Thread.sleep(800 + (int)(Math.random() * 1200)); // 0.8-2 seconds

            String messageId = "EMAIL" + String.format("%012d", System.nanoTime() % 1000000000000L);
            boolean success = Math.random() > 0.01; // 99% success rate

            if (success) {
                log.info("Onboarding email sent successfully: messageId={}", messageId);
                return new NotificationResult(true, messageId, "EMAIL", null, System.currentTimeMillis());
            } else {
                String errorMessage = "Email service temporarily unavailable";
                log.error("Email sending failed: {}", errorMessage);
                return new NotificationResult(false, null, "EMAIL", errorMessage, System.currentTimeMillis());
            }

        } catch (Exception e) {
            log.error("Email sending failed for customer: {}", customerId, e);
            return new NotificationResult(false, null, "EMAIL", "Email error: " + e.getMessage(), System.currentTimeMillis());
        }
    }
}
