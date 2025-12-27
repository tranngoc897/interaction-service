package com.ngoctran.interactionservice.workflow;

import com.ngoctran.interactionservice.WorkflowHistoryEntity;
import com.ngoctran.interactionservice.mapping.ProcessMappingEntity;
import com.ngoctran.interactionservice.mapping.ProcessMappingRepository;
import com.ngoctran.interactionservice.mapping.enums.EngineType;
import com.ngoctran.interactionservice.mapping.enums.ProcessStatus;
import com.ngoctran.interactionservice.workflow.config.TemporalWorkerConfiguration;
import com.ngoctran.interactionservice.workflow.onboarding.OnboardingWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowService {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorkflowService.class);
        private final ProcessMappingRepository processMappingRepo;


        private final WorkflowClient client;

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
                                .setTaskQueue(TemporalWorkerConfiguration.ONBOARDING_QUEUE)
                                .setWorkflowExecutionTimeout(Duration.ofDays(7))
                                .build();
                // Create onboarding stub
                OnboardingWorkflow workflow = client.newWorkflowStub(
                                OnboardingWorkflow.class,
                                options);
                // Start onboarding asynchronously
                WorkflowExecution execution = WorkflowClient
                                .start(() -> workflow.execute(caseId, interactionId, initialData));
                String processInstanceId = execution.getWorkflowId() + ":" + execution.getRunId();
                log.info("Workflow started: workflowId={}, runId={}",
                                execution.getWorkflowId(), execution.getRunId());
                // Save process mapping
                saveProcessMapping(caseId, userId, processInstanceId, "kyc-onboarding");
                return processInstanceId;
        }


        public void signalDocumentsUploaded(String workflowId, Map<String, String> documents) {
                log.info("Sending documents uploaded signal to onboarding: {}", workflowId);

                OnboardingWorkflow workflow = client.newWorkflowStub(
                                OnboardingWorkflow.class,
                                workflowId);
                workflow.documentsUploaded(documents);
                log.info("Signal sent successfully");
        }


        public void signalUserDataUpdated(String workflowId, Map<String, Object> updatedData) {
                log.info("Sending user data updated signal to onboarding: {}", workflowId);

                OnboardingWorkflow workflow = client.newWorkflowStub(
                                OnboardingWorkflow.class,
                                workflowId);
                workflow.userDataUpdated(updatedData);
                log.info("Signal sent successfully");
        }


        public void signalManualReview(String workflowId, boolean approved, String reason) {
                log.info("Sending manual review signal to onboarding: {}, approved={}", workflowId, approved);

                OnboardingWorkflow workflow = client.newWorkflowStub(
                                OnboardingWorkflow.class,
                                workflowId);
                workflow.manualReview(approved, reason);
                log.info("Signal sent successfully");
        }

        public String queryWorkflowStatus(String workflowId) {
                log.info("Querying onboarding status: {}", workflowId);
                OnboardingWorkflow workflow = client.newWorkflowStub(
                                OnboardingWorkflow.class,
                                workflowId);
                return workflow.getStatus();
        }


        public OnboardingWorkflow.WorkflowProgress queryWorkflowProgress(String workflowId) {
                log.info("Querying onboarding progress: {}", workflowId);

                OnboardingWorkflow workflow = client.newWorkflowStub(
                                OnboardingWorkflow.class,
                                workflowId);

                return workflow.getProgress();
        }


        public void cancelWorkflow(String workflowId) {
                log.info("Cancelling onboarding: {}", workflowId);

                WorkflowStub workflow = client.newUntypedWorkflowStub(workflowId);
                workflow.cancel();

                log.info("Workflow cancelled");
        }

        private void saveProcessMapping(String caseId, String userId, String processInstanceId, String processKey) {
                ProcessMappingEntity mapping = new ProcessMappingEntity();
                mapping.setId(UUID.randomUUID().toString());
                mapping.setEngineType(EngineType.TEMPORAL);
                mapping.setProcessInstanceId(processInstanceId);
                mapping.setProcessDefinitionKey(processKey);
                mapping.setCaseId(UUID.fromString(caseId));
                mapping.setUserId(userId);
                mapping.setStatus(ProcessStatus.RUNNING);
                mapping.setStartedAt(LocalDateTime.now());
                processMappingRepo.save(mapping);
                log.info("Process mapping saved: {}", mapping.getId());
        }

        public void suspendWorkflow(String workflowId) {
                log.info("Suspending workflow: {}", workflowId);

                WorkflowStub workflow = client.newUntypedWorkflowStub(workflowId);
                // In Temporal, you would use workflow.pause() or similar
                // For demo, we'll just log
                log.info("Workflow suspended: {}", workflowId);
        }


        public void resumeWorkflow(String workflowId) {
                log.info("Resuming workflow: {}", workflowId);

                WorkflowStub workflow = client.newUntypedWorkflowStub(workflowId);
                // In Temporal, you would use workflow.unpause() or similar
                log.info("Workflow resumed: {}", workflowId);
        }


        public List<WorkflowHistoryEntity> getWorkflowHistory(String workflowId) {
                log.info("Getting workflow history for: {}", workflowId);
                // In real implementation, inject WorkflowHistoryRepository
                // return workflowHistoryRepository.findByWorkflowIdOrderByChangedAtAsc(workflowId);
                // Mock empty list for demo
                return java.util.Collections.emptyList();
        }


        public Map<String, Object> getWorkflowStatistics(String workflowType, String dateRange) {
                log.info("Getting workflow statistics: type={}, dateRange={}", workflowType, dateRange);

                // In real implementation, query from WorkflowHistoryRepository
                Map<String, Object> stats = new java.util.HashMap<>();
                stats.put("totalWorkflows", 150);
                stats.put("completedWorkflows", 140);
                stats.put("failedWorkflows", 5);
                stats.put("cancelledWorkflows", 5);
                stats.put("averageExecutionTime", "45 minutes");
                stats.put("successRate", "93.3%");
                return stats;
        }

}
