package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.cases.CaseEntity;
import com.ngoctran.interactionservice.cases.CaseRepository;
import com.ngoctran.interactionservice.events.WorkflowEventPublisher;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JavaDelegate for BPMN account creation
 * Called from BPMN processes to create customer account after successful
 * onboarding
 */
@Component("accountCreationDelegate")
public class AccountCreationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(AccountCreationDelegate.class);

    private final CaseRepository caseRepository;
    private final WorkflowEventPublisher eventPublisher;

    public AccountCreationDelegate(CaseRepository caseRepository, WorkflowEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(DelegateExecution execution) {
        long startTime = System.currentTimeMillis();
        log.info("Executing account creation for process: {}", execution.getProcessInstanceId());

        try {
            // Get data from process variables
            String caseId = (String) execution.getVariable("caseId");
            String applicantId = (String) execution.getVariable("applicantId");
            @SuppressWarnings("unchecked")
            Map<String, Object> applicantData = (Map<String, Object>) execution.getVariable("applicantData");

            if (caseId == null || applicantData == null) {
                log.error("Missing required data for account creation: caseId={}, applicantData={}",
                        caseId, applicantData != null);
                execution.setVariable("accountCreated", false);
                execution.setVariable("accountCreationStatus", "FAILED");
                execution.setVariable("accountCreationError", "Missing required data");

                long duration = System.currentTimeMillis() - startTime;
                eventPublisher.publishPerformanceEvent(caseId, "ACCOUNT_CREATION", duration, "FAILED_MISSING_DATA");
                return;
            }

            // Extract customer information
            String customerName = (String) applicantData.getOrDefault("fullName", "Unknown");
            String email = (String) applicantData.getOrDefault("email", "");
            String phoneNumber = (String) applicantData.getOrDefault("phoneNumber", "");
            String idNumber = (String) applicantData.getOrDefault("idNumber", "");

            log.info("Creating account for customer: name={}, email={}, phone={}",
                    customerName, email, phoneNumber);

            // Generate account number (simplified - in real scenario, use proper account
            // number generation)
            String accountNumber = generateAccountNumber();
            String customerId = UUID.randomUUID().toString();

            // Simulate account creation in core banking system
            // In real scenario, call external banking API
            boolean accountCreationSuccess = createAccountInCoreBanking(
                    customerId, accountNumber, customerName, email, phoneNumber, idNumber);

            if (!accountCreationSuccess) {
                log.error("Failed to create account in core banking system");
                execution.setVariable("accountCreated", false);
                execution.setVariable("accountCreationStatus", "FAILED");
                execution.setVariable("accountCreationError", "Core banking system error");

                long duration = System.currentTimeMillis() - startTime;
                eventPublisher.publishPerformanceEvent(caseId, "ACCOUNT_CREATION", duration, "FAILED_CORE_BANKING");
                return;
            }

            // Update case status
            updateCaseStatus(caseId, "APPROVED");

            // Set process variables for BPMN flow
            execution.setVariable("accountCreated", true);
            execution.setVariable("accountCreationStatus", "SUCCESS");
            execution.setVariable("accountNumber", accountNumber);
            execution.setVariable("customerId", customerId);
            execution.setVariable("customerName", customerName);
            execution.setVariable("customerEmail", email);

            log.info("Account created successfully: accountNumber={}, customerId={}, customer={}",
                    accountNumber, customerId, customerName);

            // Publish case update event
            eventPublisher.publishCaseUpdateEvent(caseId, "ACCOUNT_CREATION",
                    Map.of("accountNumber", accountNumber, "customerId", customerId),
                    Map.of("status", "APPROVED", "action", "ACCOUNT_CREATED"));

            // Publish account created event
            eventPublisher.publishAccountCreatedEvent(caseId, customerId, accountNumber, customerName, "SAVINGS");

            // Publish Performance Event
            long duration = System.currentTimeMillis() - startTime;
            eventPublisher.publishPerformanceEvent(caseId, "ACCOUNT_CREATION", duration, "SUCCESS");

        } catch (Exception e) {
            log.error("Account creation failed: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;
            String caseId = (String) execution.getVariable("caseId");
            eventPublisher.publishPerformanceEvent(caseId, "ACCOUNT_CREATION", duration, "ERROR");

            execution.setVariable("accountCreated", false);
            execution.setVariable("accountCreationStatus", "ERROR");
            execution.setVariable("accountCreationError", e.getMessage());
            throw e; // Re-throw to trigger BPMN error handling
        }
    }

    /**
     * Generate unique account number
     * In real scenario, use proper account number generation logic
     */
    private String generateAccountNumber() {
        // Simple format: ACC + timestamp + random 4 digits
        long timestamp = System.currentTimeMillis() % 1000000;
        int random = (int) (Math.random() * 9000) + 1000;
        return String.format("ACC%06d%04d", timestamp, random);
    }

    /**
     * Create account in core banking system
     * This is a simulation - in real scenario, call external banking API
     */
    private boolean createAccountInCoreBanking(
            String customerId,
            String accountNumber,
            String customerName,
            String email,
            String phoneNumber,
            String idNumber) {

        try {
            log.info("Calling core banking system to create account...");
            log.info("Customer ID: {}", customerId);
            log.info("Account Number: {}", accountNumber);
            log.info("Customer Name: {}", customerName);
            log.info("Email: {}", email);
            log.info("Phone: {}", phoneNumber);
            log.info("ID Number: {}", idNumber);

            // Simulate API call delay
            Thread.sleep(500);

            // Simulate 95% success rate
            boolean success = Math.random() < 0.95;

            if (success) {
                log.info("Account created successfully in core banking system");
            } else {
                log.error("Core banking system returned error");
            }

            return success;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted during core banking API call", e);
            return false;
        } catch (Exception e) {
            log.error("Error calling core banking system: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update case status in database
     */
    private void updateCaseStatus(String caseId, String status) {
        try {
            caseRepository.findById(UUID.fromString(caseId)).ifPresent(caseEntity -> {
                caseEntity.setStatus(status);
                caseEntity.setUpdatedAt(Instant.now());
                caseRepository.save(caseEntity);
                log.info("Updated case {} status to {}", caseId, status);
            });
        } catch (Exception e) {
            log.error("Failed to update case status: {}", e.getMessage(), e);
            // Don't throw - this is not critical for account creation
        }
    }
}
