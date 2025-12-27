package com.ngoctran.interactionservice.workflow;

import com.ngoctran.interactionservice.workflow.activity.CIFActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.CleanupActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.CorebankAccountActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.IDVerificationActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.InteractionCallbackActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.KeycloakAccountActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.NotificationActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.OCRActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.OnboardingNotificationActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.RetailAccountActivityImpl;
import com.ngoctran.interactionservice.workflow.activity.TaskActivityImpl;
import com.ngoctran.interactionservice.workflow.onboarding.DocumentProcessingWorkflowImpl;
import com.ngoctran.interactionservice.workflow.onboarding.DynamicStepWorkflowImpl;
import com.ngoctran.interactionservice.workflow.onboarding.OnboardingWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WorkerConfiguration {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorkerConfiguration.class);

    private final WorkerFactory workerFactory;
    // Activity implementations
    private final OCRActivityImpl ocrActivity;
    private final IDVerificationActivityImpl idVerificationActivity;
    private final NotificationActivityImpl notificationActivity;
    private final InteractionCallbackActivityImpl interactionCallbackActivity;
    private final TaskActivityImpl taskActivity;
    private final CleanupActivityImpl cleanupActivity;
    private final CIFActivityImpl cifActivity;
    private final CorebankAccountActivityImpl corebankAccountActivity;
    private final RetailAccountActivityImpl retailAccountActivity;
    private final KeycloakAccountActivityImpl keycloakAccountActivity;
    private final OnboardingNotificationActivityImpl onboardingNotificationActivity;

    /**
     * Task Queue Names
     */
    public static final String KYC_ONBOARDING_QUEUE = "KYC_ONBOARDING_QUEUE";
    public static final String DOCUMENT_VERIFICATION_QUEUE = "DOCUMENT_VERIFICATION_QUEUE";
    public static final String GENERAL_QUEUE = "GENERAL_QUEUE";
    public static final String RECONCILIATION_QUEUE = "RECONCILIATION_QUEUE";

    @PostConstruct
    public void registerWorkersAndActivities() {
        log.info("Registering Temporal Workers and Activities...");
        // Register KYC Onboarding Worker
        registerKYCOnboardingWorker();
        // Register Document Verification Worker
        registerDocumentVerificationWorker();
        // Register General Worker
        registerGeneralWorker();
        // Register Reconciliation Worker
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
                OnboardingWorkflowImpl.class,
                DocumentProcessingWorkflowImpl.class,
                DynamicStepWorkflowImpl.class);
        // Register activity implementations
        worker.registerActivitiesImplementations(
                ocrActivity,
                idVerificationActivity,
                notificationActivity,
                interactionCallbackActivity,
                taskActivity,
                // Onboarding activities
                cifActivity,
                corebankAccountActivity,
                retailAccountActivity,
                keycloakAccountActivity,
                onboardingNotificationActivity);

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
        // Register activity implementations
        worker.registerActivitiesImplementations(
            notificationActivity,
            interactionCallbackActivity,
            cleanupActivity
        );
    }

}
