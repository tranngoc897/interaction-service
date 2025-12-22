package com.ngoctran.interactionservice.workflow.activity;

import org.springframework.stereotype.Component;

@Component
public class NotificationActivityImpl implements NotificationActivity {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationActivityImpl.class);
    
    @Override
    public void sendNotification(String caseId, String notificationType, String message) {
        log.info("Sending notification: caseId={}, type={}, message={}", caseId, notificationType, message);
        
        // TODO: Integrate with notification service (Firebase, SNS, Twilio, etc.)
        // For now, just log
        
        log.info("Notification sent successfully");
    }
    
    @Override
    public void sendEmail(String email, String subject, String body) {
        log.info("Sending email to: {}, subject: {}", email, subject);
        
        // TODO: Integrate with email service (SendGrid, SES, etc.)
        
        log.info("Email sent successfully");
    }
    
    @Override
    public void sendSMS(String phoneNumber, String message) {
        log.info("Sending SMS to: {}, message: {}", phoneNumber, message);
        
        // TODO: Integrate with SMS service (Twilio, SNS, etc.)
        
        log.info("SMS sent successfully");
    }
}
