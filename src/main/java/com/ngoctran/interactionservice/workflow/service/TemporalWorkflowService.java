package com.ngoctran.interactionservice.workflow.service;

import com.ngoctran.interactionservice.mapping.ProcessMappingEntity;
import com.ngoctran.interactionservice.mapping.ProcessMappingRepository;
import com.ngoctran.interactionservice.mapping.enums.EngineType;
import com.ngoctran.interactionservice.mapping.enums.ProcessStatus;
import com.ngoctran.interactionservice.workflow.config.WorkerConfiguration;
import com.ngoctran.interactionservice.workflow.onboarding.KYCOnboardingWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemporalWorkflowService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TemporalWorkflowService.class);
    private final WorkflowClient workflowClient;
    private final ProcessMappingRepository processMappingRepo;
    
    /**
     * Start KYC Onboarding Workflow
     */
    @Transactional
    public String startKYCOnboardingWorkflow(
            String caseId,
            String interactionId,
            String userId,
            Map<String, Object> initialData) {
        
        log.info("Starting KYC Onboarding Workflow: caseId={}, interactionId={}", caseId, interactionId);
        // Create onboarding options
        String workflowId = "kyc-onboarding-" + caseId;
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(WorkerConfiguration.KYC_ONBOARDING_QUEUE)
                .setWorkflowExecutionTimeout(Duration.ofDays(7))
                .build();
        // Create onboarding stub
        KYCOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                KYCOnboardingWorkflow.class,
                options
        );
        // Start onboarding asynchronously
        WorkflowExecution execution = WorkflowClient.start(() -> workflow.execute(caseId, interactionId, initialData)
        );
        String processInstanceId = execution.getWorkflowId() + ":" + execution.getRunId();
        log.info("Workflow started: workflowId={}, runId={}", 
                execution.getWorkflowId(), execution.getRunId());
        // Save process mapping
        saveProcessMapping(caseId, userId, processInstanceId, "kyc-onboarding");
        return processInstanceId;
    }
    
    /**
     * Signal: Documents uploaded
     */
    public void signalDocumentsUploaded(String workflowId, Map<String, String> documents) {
        log.info("Sending documents uploaded signal to onboarding: {}", workflowId);
        
        KYCOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                KYCOnboardingWorkflow.class,
                workflowId
        );
        workflow.documentsUploaded(documents);
        log.info("Signal sent successfully");
    }
    
    /**
     * Signal: User data updated
     */
    public void signalUserDataUpdated(String workflowId, Map<String, Object> updatedData) {
        log.info("Sending user data updated signal to onboarding: {}", workflowId);
        
        KYCOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                KYCOnboardingWorkflow.class,
                workflowId
        );
        workflow.userDataUpdated(updatedData);
        log.info("Signal sent successfully");
    }
    
    /**
     * Signal: Manual review
     */
    public void signalManualReview(String workflowId, boolean approved, String reason) {
        log.info("Sending manual review signal to onboarding: {}, approved={}", workflowId, approved);
        
        KYCOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                KYCOnboardingWorkflow.class,
                workflowId
        );
        workflow.manualReview(approved, reason);
        log.info("Signal sent successfully");
    }
    
    /**
     * Query onboarding status
     */
    public String queryWorkflowStatus(String workflowId) {
        log.info("Querying onboarding status: {}", workflowId);
        KYCOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                KYCOnboardingWorkflow.class,
                workflowId
        );
        return workflow.getStatus();
    }
    
    /**
     * Query onboarding progress
     */
    public KYCOnboardingWorkflow.WorkflowProgress queryWorkflowProgress(String workflowId) {
        log.info("Querying onboarding progress: {}", workflowId);
        
        KYCOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                KYCOnboardingWorkflow.class,
                workflowId
        );
        
        return workflow.getProgress();
    }
    
    /**
     * Cancel onboarding
     */
    public void cancelWorkflow(String workflowId) {
        log.info("Cancelling onboarding: {}", workflowId);
        
        WorkflowStub workflow = workflowClient.newUntypedWorkflowStub(workflowId);
        workflow.cancel();
        
        log.info("Workflow cancelled");
    }
    
    private void saveProcessMapping(String caseId, String userId, String processInstanceId, String processKey) {
        ProcessMappingEntity mapping = new ProcessMappingEntity();
        mapping.setId(UUID.randomUUID().toString());
        mapping.setEngineType(EngineType.TEMPORAL);
        mapping.setProcessInstanceId(processInstanceId);
        mapping.setProcessDefinitionKey(processKey);
        mapping.setCaseId(caseId);
        mapping.setUserId(userId);
        mapping.setStatus(ProcessStatus.RUNNING);
        mapping.setStartedAt(LocalDateTime.now());
        
        processMappingRepo.save(mapping);
        
        log.info("Process mapping saved: {}", mapping.getId());
    }
}
