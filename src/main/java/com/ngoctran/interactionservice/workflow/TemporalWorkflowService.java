package com.ngoctran.interactionservice.workflow;

import com.ngoctran.interactionservice.mapping.ProcessMappingEntity;
import com.ngoctran.interactionservice.mapping.ProcessMappingRepository;
import com.ngoctran.interactionservice.mapping.enums.EngineType;
import com.ngoctran.interactionservice.mapping.enums.ProcessStatus;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.schedules.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemporalWorkflowService {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TemporalWorkflowService.class);
        private final WorkflowClient workflowClient;
        private final ScheduleClient scheduleClient;
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

        /**
         * Start Advanced Dynamic Pipeline Workflow
         */
        public String startAdvancedPipeline(String pipelineName, List<Map<String, Object>> tasks,
                        Map<String, Object> inputData) {
                log.info("Starting Advanced Pipeline: {}", pipelineName);

                String workflowId = "pipeline-" + UUID.randomUUID().toString();
                WorkflowOptions options = WorkflowOptions.newBuilder()
                                .setWorkflowId(workflowId)
                                .setTaskQueue(WorkerConfiguration.GENERAL_QUEUE)
                                .build();

                AdvancedPipelineWorkflow workflow = workflowClient.newWorkflowStub(AdvancedPipelineWorkflow.class,
                                options);

                // Start it and return ID
                WorkflowClient.start(workflow::runPipeline, pipelineName, tasks, inputData);
                return workflowId;
        }

        /**
         * Signal: Documents uploaded
         */
        public void signalDocumentsUploaded(String workflowId, Map<String, String> documents) {
                log.info("Sending documents uploaded signal to onboarding: {}", workflowId);

                KYCOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                                KYCOnboardingWorkflow.class,
                                workflowId);
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
                                workflowId);
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
                                workflowId);
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
                                workflowId);
                return workflow.getStatus();
        }

        /**
         * Query onboarding progress
         */
        public KYCOnboardingWorkflow.WorkflowProgress queryWorkflowProgress(String workflowId) {
                log.info("Querying onboarding progress: {}", workflowId);

                KYCOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                                KYCOnboardingWorkflow.class,
                                workflowId);

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
                mapping.setCaseId(UUID.fromString(caseId));
                mapping.setUserId(userId);
                mapping.setStatus(ProcessStatus.RUNNING);
                mapping.setStartedAt(LocalDateTime.now());

                processMappingRepo.save(mapping);

                log.info("Process mapping saved: {}", mapping.getId());
        }

        /**
         * Create a Schedule for the Cleanup Workflow
         */
        public void createCleanupSchedule() {
                log.info("Creating/Updating Cleanup Workflow Schedule...");

                ScheduleActionStartWorkflow action = ScheduleActionStartWorkflow.newBuilder()
                                .setWorkflowType(CleanupWorkflow.class)
                                .setOptions(WorkflowOptions.newBuilder()
                                                .setWorkflowId("scheduled-cleanup-job")
                                                .setTaskQueue(WorkerConfiguration.GENERAL_QUEUE)
                                                .build())
                                .build();

                Schedule schedule = Schedule.newBuilder()
                                .setAction(action)
                                .setSpec(ScheduleSpec.newBuilder()
                                                // Run every hour
                                                .setIntervals(Collections.singletonList(
                                                                new ScheduleIntervalSpec(Duration.ofHours(1))))
                                                .build())
                                .build();

                try {
                        // Create the schedule (throws error if exists, which we catch)
                        scheduleClient.createSchedule(
                                        "DAILY_CLEANUP_SCHEDULE",
                                        schedule,
                                        ScheduleOptions.newBuilder().build());
                        log.info("Cleanup schedule created successfully");
                } catch (Exception e) {
                        log.info("Schedule already exists or error occurred: {}", e.getMessage());
                }
        }

        /**
         * Create a Schedule for an Onboarding Monitor job
         */
        public void createOnboardingMonitorSchedule(String scheduleId, String cronSchedule) {
                log.info("Creating Onboarding Monitor Schedule: {} with cron: {}", scheduleId, cronSchedule);
                ScheduleActionStartWorkflow action = ScheduleActionStartWorkflow.newBuilder()
                                .setWorkflowType(CaseMonitorWorkflow.class)
                                .setOptions(WorkflowOptions.newBuilder()
                                                .setWorkflowId("onboarding-monitor-" + scheduleId)
                                                .setTaskQueue(WorkerConfiguration.KYC_ONBOARDING_QUEUE)
                                                .build())
                                // Default arguments for monitorCase(String caseId, int iterationCount)
                                .setArguments("SYSTEM_WIDE", 0)
                                .build();

                Schedule schedule = Schedule.newBuilder()
                                .setAction(action)
                                .setSpec(ScheduleSpec.newBuilder()
                                                .setCronExpressions(Collections.singletonList(cronSchedule))
                                                .build()).build();

                try {
                        scheduleClient.createSchedule(scheduleId, schedule, ScheduleOptions.newBuilder().build());
                        log.info("Onboarding monitor schedule {} created successfully", scheduleId);

                } catch (Exception e) {
                        log.warn("Schedule {} might already exist: {}", scheduleId, e.getMessage());
                        // In a real app, you might want to update it
                }

        }

}
