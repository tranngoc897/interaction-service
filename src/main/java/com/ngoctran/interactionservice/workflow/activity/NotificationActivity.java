package com.ngoctran.interactionservice.workflow.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface NotificationActivity {
    
    @ActivityMethod
    void sendNotification(String caseId, String notificationType, String message);
    
    @ActivityMethod
    void sendEmail(String email, String subject, String body);
    
    @ActivityMethod
    void sendSMS(String phoneNumber, String message);
}
