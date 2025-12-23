package com.ngoctran.interactionservice.workflow;

import com.ngoctran.interactionservice.workflow.activity.*;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Worker Configuration for Temporal
 * 
 * Registers workflows and activities with different task queues:
 * - KYC_ONBOARDING_QUEUE: KYC onboarding workflows
 * - DOCUMENT_VERIFICATION_QUEUE: Document verification workflows
 * - GENERAL_QUEUE: General purpose workflows
 */
@Configuration
@RequiredArgsConstructor
public class WorkerConfiguration {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorkerConfiguration.class);

    private final WorkerFactory workerFactory;
    private final WorkflowClient workflowClient;

    // Activity implementations
    private final OCRActivityImpl ocrActivity;
    private final IDVerificationActivityImpl idVerificationActivity;
    private final NotificationActivityImpl notificationActivity;
    private final InteractionCallbackActivityImpl interactionCallbackActivity;
    private final TaskActivityImpl taskActivity;
    private final CleanupActivityImpl cleanupActivity;

    /**
     * Task Queue Names
     */
    public static final String KYC_ONBOARDING_QUEUE = "KYC_ONBOARDING_QUEUE";
    public static final String DOCUMENT_VERIFICATION_QUEUE = "DOCUMENT_VERIFICATION_QUEUE";
    public static final String GENERAL_QUEUE = "GENERAL_QUEUE";

    @PostConstruct
    public void registerWorkersAndActivities() {
        log.info("Registering Temporal Workers and Activities...");
        // Register KYC Onboarding Worker
        registerKYCOnboardingWorker();
        // Register Document Verification Worker
        registerDocumentVerificationWorker();
        // Register General Worker
        registerGeneralWorker();
        // Start all workers
        workerFactory.start();

        log.info("All Temporal Workers started successfully");
    }

    /**
     * Register KYC Onboarding Worker
     * Handles KYC onboarding workflows
     */
    private void registerKYCOnboardingWorker() {
        log.info("Registering KYC Onboarding Worker on queue: {}", KYC_ONBOARDING_QUEUE);
        Worker worker = workerFactory.newWorker(KYC_ONBOARDING_QUEUE);
        // Register onboarding implementations
        worker.registerWorkflowImplementationTypes(
                KYCOnboardingWorkflowImpl.class,
                DocumentProcessingWorkflowImpl.class);
        // Register activity implementations
        worker.registerActivitiesImplementations(
                ocrActivity,
                idVerificationActivity,
                notificationActivity,
                interactionCallbackActivity,
                taskActivity);

        log.info("KYC Onboarding Worker registered successfully");
    }

    /**
     * Register Document Verification Worker
     * Handles document verification workflows
     */
    private void registerDocumentVerificationWorker() {
        log.info("Registering Document Verification Worker on queue: {}", DOCUMENT_VERIFICATION_QUEUE);
        Worker worker = workerFactory.newWorker(DOCUMENT_VERIFICATION_QUEUE);
        // Register onboarding implementations
        // worker.registerWorkflowImplementationTypes(DocumentVerificationWorkflowImpl.class);
        // Register activity implementations
        worker.registerActivitiesImplementations(
                ocrActivity,
                idVerificationActivity,
                notificationActivity,
                interactionCallbackActivity);

        log.info("Document Verification Worker registered successfully");
    }

    /**
     * Register General Worker
     * Handles general purpose workflows
     */
    private void registerGeneralWorker() {
        log.info("Registering General Worker on queue: {}", GENERAL_QUEUE);

        Worker worker = workerFactory.newWorker(GENERAL_QUEUE);

        // Register Scheduled Workflow
        worker.registerWorkflowImplementationTypes(CleanupWorkflowImpl.class);

        // Register activity implementations
        worker.registerActivitiesImplementations(
                notificationActivity,
                interactionCallbackActivity,
                cleanupActivity);

        log.info("General Worker registered successfully");
    }
}
