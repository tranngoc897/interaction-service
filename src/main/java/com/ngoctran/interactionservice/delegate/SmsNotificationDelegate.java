package com.ngoctran.interactionservice.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaDelegate for SMS notifications
 * Sends SMS messages to customers at various stages of onboarding
 */
@Component("smsNotificationDelegate")
public class SmsNotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationDelegate.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing SMS notification for process: {}", execution.getProcessInstanceId());

        try {
            // Get notification details from process variables
            String smsType = (String) execution.getVariable("smsType");
            if (smsType == null) {
                smsType = "WELCOME_SMS"; // Default
            }

            String phoneNumber = (String) execution.getVariable("customerPhone");
            String customerName = (String) execution.getVariable("customerName");
            String accountNumber = (String) execution.getVariable("accountNumber");

            if (phoneNumber == null || phoneNumber.isEmpty()) {
                log.warn("No phone number found for SMS notification");
                execution.setVariable("smsSent", false);
                execution.setVariable("smsStatus", "NO_PHONE_NUMBER");
                return;
            }

            log.info("Sending {} SMS to: {}", smsType, phoneNumber);

            // Send SMS based on type
            boolean sent = false;
            String smsContent = "";

            switch (smsType) {
                case "WELCOME_SMS":
                    smsContent = buildWelcomeSms(customerName, accountNumber);
                    sent = sendSms(phoneNumber, smsContent);
                    break;

                case "OTP_VERIFICATION":
                    String otp = generateOtp();
                    execution.setVariable("generatedOtp", otp);
                    smsContent = buildOtpSms(otp);
                    sent = sendSms(phoneNumber, smsContent);
                    break;

                case "DOCUMENT_UPLOAD_REMINDER":
                    smsContent = buildDocumentReminderSms(customerName);
                    sent = sendSms(phoneNumber, smsContent);
                    break;

                case "APPLICATION_APPROVED":
                    smsContent = buildApprovalSms(customerName, accountNumber);
                    sent = sendSms(phoneNumber, smsContent);
                    break;

                case "APPLICATION_REJECTED":
                    smsContent = buildRejectionSms(customerName);
                    sent = sendSms(phoneNumber, smsContent);
                    break;

                case "MANUAL_REVIEW_NOTIFICATION":
                    smsContent = buildManualReviewSms(customerName);
                    sent = sendSms(phoneNumber, smsContent);
                    break;

                case "ACCOUNT_ACTIVATED":
                    smsContent = buildAccountActivationSms(customerName, accountNumber);
                    sent = sendSms(phoneNumber, smsContent);
                    break;

                default:
                    log.warn("Unknown SMS type: {}", smsType);
                    smsContent = buildGenericSms(customerName);
                    sent = sendSms(phoneNumber, smsContent);
            }

            // Set process variables for BPMN flow
            execution.setVariable("smsSent", sent);
            execution.setVariable("smsStatus", sent ? "SENT" : "FAILED");
            execution.setVariable("smsTimestamp", LocalDateTime.now().toString());
            execution.setVariable("smsContent", smsContent);

            log.info("SMS {} sent successfully: {}", smsType, sent);

        } catch (Exception e) {
            log.error("SMS sending failed: {}", e.getMessage(), e);
            execution.setVariable("smsSent", false);
            execution.setVariable("smsStatus", "ERROR");
            execution.setVariable("smsError", e.getMessage());
            // Don't throw - SMS failure shouldn't stop the process
        }
    }

    /**
     * Send SMS via SMS gateway (simulated)
     */
    private boolean sendSms(String phoneNumber, String message) {
        try {
            log.info("=== SMS NOTIFICATION ===");
            log.info("To: {}", phoneNumber);
            log.info("Message: {}", message);
            log.info("Length: {} characters", message.length());
            log.info("========================");

            // Simulate SMS gateway API call
            Thread.sleep(300);

            // Simulate 97% success rate
            boolean success = Math.random() < 0.97;

            if (success) {
                log.info("SMS sent successfully to: {}", phoneNumber);
            } else {
                log.error("SMS gateway returned error for: {}", phoneNumber);
            }

            return success;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted during SMS sending", e);
            return false;
        } catch (Exception e) {
            log.error("Error sending SMS: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate 6-digit OTP
     */
    private String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }

    // ===== SMS Content Builders =====

    private String buildWelcomeSms(String customerName, String accountNumber) {
        return String.format(
                "Welcome to Our Bank, %s! Your account %s has been created. Download our app to get started.",
                customerName != null ? customerName : "Customer",
                accountNumber != null ? accountNumber : "is being processed");
    }

    private String buildOtpSms(String otp) {
        return String.format(
                "Your OTP for bank account verification is: %s. Valid for 5 minutes. Do not share this code.",
                otp);
    }

    private String buildDocumentReminderSms(String customerName) {
        return String.format(
                "Hi %s, please upload your required documents to complete your bank account application. Visit our app or website.",
                customerName != null ? customerName : "Customer");
    }

    private String buildApprovalSms(String customerName, String accountNumber) {
        return String.format(
                "Congratulations %s! Your account application has been approved. Account Number: %s. Welcome to Our Bank!",
                customerName != null ? customerName : "Customer",
                accountNumber != null ? accountNumber : "will be provided shortly");
    }

    private String buildRejectionSms(String customerName) {
        return String.format(
                "Dear %s, we regret to inform you that we cannot proceed with your application at this time. Contact us for more info.",
                customerName != null ? customerName : "Customer");
    }

    private String buildManualReviewSms(String customerName) {
        return String.format(
                "Hi %s, your application is under review. We'll notify you within 1-2 business days. Thank you for your patience.",
                customerName != null ? customerName : "Customer");
    }

    private String buildAccountActivationSms(String customerName, String accountNumber) {
        return String.format(
                "Hi %s, your account %s is now active! You can start banking with us. Download our mobile app today.",
                customerName != null ? customerName : "Customer",
                accountNumber != null ? accountNumber : "");
    }

    private String buildGenericSms(String customerName) {
        return String.format(
                "Hi %s, this is a notification from Our Bank regarding your account application. Contact us for details.",
                customerName != null ? customerName : "Customer");
    }
}
