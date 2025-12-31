package com.ngoctran.interactionservice.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaDelegate for BPMN notifications
 * Called from BPMN processes to send email/SMS notifications
 */
@Component("notificationDelegate")
public class NotificationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(NotificationDelegate.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing notification for process: {}", execution.getProcessInstanceId());

        try {
            // Get notification type from process variables
            String notificationType = (String) execution.getVariable("notificationType");
            if (notificationType == null) {
                notificationType = "WELCOME_EMAIL"; // Default
            }

            // Get recipient information
            String customerEmail = (String) execution.getVariable("customerEmail");
            String customerName = (String) execution.getVariable("customerName");
            String accountNumber = (String) execution.getVariable("accountNumber");

            if (customerEmail == null || customerEmail.isEmpty()) {
                log.warn("No customer email found for notification");
                execution.setVariable("notificationSent", false);
                execution.setVariable("notificationStatus", "SKIPPED");
                return;
            }

            log.info("Sending {} notification to: {} ({})", notificationType, customerName, customerEmail);

            // Send notification based on type
            boolean sent = false;
            switch (notificationType) {
                case "WELCOME_EMAIL":
                    sent = sendWelcomeEmail(customerEmail, customerName, accountNumber);
                    break;
                case "ACCOUNT_CREATED":
                    sent = sendAccountCreatedNotification(customerEmail, customerName, accountNumber);
                    break;
                case "MANUAL_REVIEW_REQUIRED":
                    sent = sendManualReviewNotification(customerEmail, customerName);
                    break;
                case "ONBOARDING_REJECTED":
                    sent = sendRejectionNotification(customerEmail, customerName);
                    break;
                case "COMPLIANCE_ALERT":
                    sent = sendComplianceAlert(customerEmail, customerName);
                    break;
                default:
                    log.warn("Unknown notification type: {}", notificationType);
                    sent = sendGenericNotification(customerEmail, customerName);
            }

            // Set process variables for BPMN flow
            execution.setVariable("notificationSent", sent);
            execution.setVariable("notificationStatus", sent ? "SENT" : "FAILED");
            execution.setVariable("notificationTimestamp", LocalDateTime.now().toString());

            log.info("Notification {} sent successfully: {}", notificationType, sent);

        } catch (Exception e) {
            log.error("Notification sending failed: {}", e.getMessage(), e);
            execution.setVariable("notificationSent", false);
            execution.setVariable("notificationStatus", "ERROR");
            execution.setVariable("notificationError", e.getMessage());
            // Don't throw - notification failure shouldn't stop the process
        }
    }

    /**
     * Send welcome email to new customer
     */
    private boolean sendWelcomeEmail(String email, String customerName, String accountNumber) {
        try {
            log.info("Sending welcome email to: {}", email);

            String subject = "Welcome to Our Bank!";
            String body = buildWelcomeEmailBody(customerName, accountNumber);

            return sendEmail(email, subject, body);

        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send account created notification
     */
    private boolean sendAccountCreatedNotification(String email, String customerName, String accountNumber) {
        try {
            log.info("Sending account created notification to: {}", email);

            String subject = "Your Account Has Been Created";
            String body = buildAccountCreatedBody(customerName, accountNumber);

            return sendEmail(email, subject, body);

        } catch (Exception e) {
            log.error("Failed to send account created notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send manual review notification
     */
    private boolean sendManualReviewNotification(String email, String customerName) {
        try {
            log.info("Sending manual review notification to: {}", email);

            String subject = "Your Application is Under Review";
            String body = buildManualReviewBody(customerName);

            return sendEmail(email, subject, body);

        } catch (Exception e) {
            log.error("Failed to send manual review notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send rejection notification
     */
    private boolean sendRejectionNotification(String email, String customerName) {
        try {
            log.info("Sending rejection notification to: {}", email);

            String subject = "Application Status Update";
            String body = buildRejectionBody(customerName);

            return sendEmail(email, subject, body);

        } catch (Exception e) {
            log.error("Failed to send rejection notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send compliance alert
     */
    private boolean sendComplianceAlert(String email, String customerName) {
        try {
            log.info("Sending compliance alert to: {}", email);

            String subject = "Additional Information Required";
            String body = buildComplianceAlertBody(customerName);

            return sendEmail(email, subject, body);

        } catch (Exception e) {
            log.error("Failed to send compliance alert: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send generic notification
     */
    private boolean sendGenericNotification(String email, String customerName) {
        try {
            log.info("Sending generic notification to: {}", email);

            String subject = "Notification from Our Bank";
            String body = buildGenericBody(customerName);

            return sendEmail(email, subject, body);

        } catch (Exception e) {
            log.error("Failed to send generic notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send email (simulation)
     * In real scenario, integrate with email service (SendGrid, AWS SES, etc.)
     */
    private boolean sendEmail(String to, String subject, String body) {
        try {
            log.info("=== EMAIL NOTIFICATION ===");
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("Body:\n{}", body);
            log.info("========================");

            // Simulate email sending delay
            Thread.sleep(200);

            // Simulate 98% success rate
            boolean success = Math.random() < 0.98;

            if (success) {
                log.info("Email sent successfully to: {}", to);
            } else {
                log.error("Email service returned error for: {}", to);
            }

            return success;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted during email sending", e);
            return false;
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            return false;
        }
    }

    // ===== Email Body Builders =====

    private String buildWelcomeEmailBody(String customerName, String accountNumber) {
        return String.format("""
                Dear %s,

                Welcome to Our Bank! We are delighted to have you as our customer.

                Your account has been successfully created:
                Account Number: %s
                Created Date: %s

                You can now access all our banking services through:
                - Mobile Banking App
                - Internet Banking
                - ATM Network

                If you have any questions, please don't hesitate to contact our customer service.

                Best regards,
                Customer Onboarding Team
                """,
                customerName,
                accountNumber != null ? accountNumber : "Pending",
                LocalDateTime.now().format(DATE_FORMATTER));
    }

    private String buildAccountCreatedBody(String customerName, String accountNumber) {
        return String.format("""
                Dear %s,

                Your account has been successfully created!

                Account Details:
                - Account Number: %s
                - Account Type: Savings Account
                - Status: Active
                - Created: %s

                Next Steps:
                1. Set up your online banking credentials
                2. Download our mobile app
                3. Visit any branch to activate your debit card

                Thank you for choosing our bank!

                Best regards,
                Account Services Team
                """,
                customerName,
                accountNumber,
                LocalDateTime.now().format(DATE_FORMATTER));
    }

    private String buildManualReviewBody(String customerName) {
        return String.format("""
                Dear %s,

                Thank you for your application.

                Your application is currently under manual review by our compliance team.
                This is a standard procedure to ensure the security and accuracy of your information.

                Expected Review Time: 1-2 business days

                We will notify you once the review is complete.

                If you have any questions, please contact our support team.

                Best regards,
                Compliance Team
                """,
                customerName);
    }

    private String buildRejectionBody(String customerName) {
        return String.format("""
                Dear %s,

                Thank you for your interest in opening an account with us.

                After careful review, we regret to inform you that we are unable to proceed
                with your application at this time.

                If you believe this decision was made in error or would like more information,
                please contact our customer service team.

                We appreciate your understanding.

                Best regards,
                Customer Service Team
                """,
                customerName);
    }

    private String buildComplianceAlertBody(String customerName) {
        return String.format("""
                Dear %s,

                We need additional information to complete your application.

                Please provide the following:
                - Additional identification documents
                - Proof of address
                - Source of funds documentation

                You can upload these documents through our mobile app or visit any branch.

                Please submit within 7 days to avoid application cancellation.

                Best regards,
                Compliance Team
                """,
                customerName);
    }

    private String buildGenericBody(String customerName) {
        return String.format("""
                Dear %s,

                This is a notification regarding your account application.

                For more information, please contact our customer service team
                or visit our nearest branch.

                Best regards,
                Customer Service Team
                """,
                customerName);
    }
}
