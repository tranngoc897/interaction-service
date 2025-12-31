package com.ngoctran.interactionservice.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Notification Service - Handles sending SMS and Email notifications
 * In a real scenario, this would integrate with Twilio, SendGrid, or AWS SES
 */
@Service
@Slf4j
public class NotificationService {

    public void sendEmail(String to, String subject, String body) {
        log.info("Sending EMAIL to: {} | Subject: {} | Body: {}", to, subject, body);
        // Add real integration here
    }

    public void sendSms(String phoneNumber, String message) {
        log.info("Sending SMS to: {} | Message: {}", phoneNumber, message);
        // Add real integration here
    }

    public void processMilestoneNotification(String caseId, String milestoneKey, String eventType,
            Map<String, Object> data) {
        log.info("Processing milestone notification for case: {} milestone: {}", caseId, milestoneKey);

        String recipient = (String) data.getOrDefault("email", "customer@example.com");

        if ("COMPLETED".equals(eventType)) {
            String subject = "Milestone Reached: " + milestoneKey;
            String body = String.format(
                    "Dear Customer, your milestone '%s' for case %s has been successfully completed.",
                    milestoneKey, caseId);
            sendEmail(recipient, subject, body);
        } else if ("FAILED".equals(eventType)) {
            String subject = "Action Required: Milestone Failed";
            String body = String.format(
                    "Dear Customer, the milestone '%s' for case %s has failed. Please contact support.",
                    milestoneKey, caseId);
            sendEmail(recipient, subject, body);
        }
    }
}
