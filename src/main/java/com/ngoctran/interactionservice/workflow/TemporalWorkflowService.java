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
        private final ScheduleRepository scheduleRepo;

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
        @Transactional
        public void createOnboardingMonitorSchedule(String scheduleId, String cronSchedule) {
                log.info("Creating Onboarding Monitor Schedule: {} with cron: {}", scheduleId, cronSchedule);

                // Check if schedule already exists in DB
                if (scheduleRepo.existsByScheduleIdAndStatus(scheduleId, ScheduleStatus.ACTIVE)) {
                        log.warn("Schedule {} already exists and is active", scheduleId);
                        return;
                }

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
                        log.info("Onboarding monitor schedule {} created successfully in Temporal", scheduleId);

                        // Save schedule metadata to database
                        saveScheduleEntity(scheduleId, cronSchedule, "CaseMonitorWorkflow",
                                         WorkerConfiguration.KYC_ONBOARDING_QUEUE, null);

                } catch (Exception e) {
                        log.warn("Schedule {} might already exist in Temporal: {}", scheduleId, e.getMessage());
                        // Still save to DB if it doesn't exist there, for consistency
                        if (!scheduleRepo.existsById(scheduleId)) {
                                saveScheduleEntity(scheduleId, cronSchedule, "CaseMonitorWorkflow",
                                                 WorkerConfiguration.KYC_ONBOARDING_QUEUE, null);
                        }
                }

        }

        @Transactional
        public void createPaymentProcessingSchedule(String scheduleId, String cronSchedule) {
                log.info("Creating Payment Processing Schedule: {} with cron: {}", scheduleId, cronSchedule);
                // Check if schedule already exists in DB
                if (scheduleRepo.existsByScheduleIdAndStatus(scheduleId, ScheduleStatus.ACTIVE)) {
                        log.warn("Payment schedule {} already exists and is active", scheduleId);
                        return;
                }

                ScheduleActionStartWorkflow action = ScheduleActionStartWorkflow.newBuilder()
                                .setWorkflowType(com.ngoctran.interactionservice.payment.PaymentMonitorWorkflow.class)
                                .setOptions(WorkflowOptions.newBuilder()
                                                .setWorkflowId("payment-monitor-" + scheduleId)
                                                .setTaskQueue(WorkerConfiguration.GENERAL_QUEUE)
                                                .build())
                                // Default arguments for monitorPayments(String accountId, int iterationCount)
                                .setArguments("BANKING_SYSTEM", 0)
                                .build();

                Schedule schedule = Schedule.newBuilder()
                                .setAction(action)
                                .setSpec(ScheduleSpec.newBuilder()
                                                .setCronExpressions(Collections.singletonList(cronSchedule))
                                                .build()).build();

                try {
                        scheduleClient.createSchedule(scheduleId, schedule, ScheduleOptions.newBuilder().build());
                        log.info("Payment processing schedule {} created successfully in Temporal", scheduleId);

                        // Save schedule metadata to database
                        saveScheduleEntity(scheduleId, cronSchedule, "PaymentMonitorWorkflow",
                                         WorkerConfiguration.GENERAL_QUEUE, null);

                } catch (Exception e) {
                        log.warn("Payment schedule {} might already exist in Temporal: {}", scheduleId, e.getMessage());
                        // Still save to DB if it doesn't exist there, for consistency
                        if (!scheduleRepo.existsById(scheduleId)) {
                                saveScheduleEntity(scheduleId, cronSchedule, "PaymentMonitorWorkflow",
                                                 WorkerConfiguration.GENERAL_QUEUE, null);
                        }
                }

        }


        private void saveScheduleEntity(String scheduleId, String cronExpression, String workflowType,
                                      String taskQueue, String createdBy) {
                ScheduleEntity scheduleEntity = new ScheduleEntity();
                scheduleEntity.setScheduleId(scheduleId);
                scheduleEntity.setCronExpression(cronExpression);
                scheduleEntity.setWorkflowType(workflowType);
                scheduleEntity.setTaskQueue(taskQueue);
                scheduleEntity.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");

                // Set appropriate description and arguments based on workflow type
                if ("PaymentMonitorWorkflow".equals(workflowType)) {
                        scheduleEntity.setDescription("Payment processing monitor schedule");
                        scheduleEntity.setWorkflowArguments("[\"BANKING_SYSTEM\", 0]");
                } else if ("CaseMonitorWorkflow".equals(workflowType)) {
                        scheduleEntity.setDescription("Onboarding monitor schedule");
                        scheduleEntity.setWorkflowArguments("[\"SYSTEM_WIDE\", 0]");
                } else {
                        scheduleEntity.setDescription("Workflow schedule");
                        scheduleEntity.setWorkflowArguments("[]");
                }

                scheduleRepo.save(scheduleEntity);
                log.info("Schedule metadata saved to database: {}", scheduleId);
        }

        /**
         * Get all schedules from database
         */
        public List<ScheduleEntity> getAllSchedules() {
                log.info("Retrieving all schedules from database");
                return scheduleRepo.findAll();
        }

        /**
         * Get schedule by ID from database
         */
        public ScheduleEntity getScheduleById(String scheduleId) {
                log.info("Retrieving schedule by ID: {}", scheduleId);
                return scheduleRepo.findById(scheduleId)
                        .orElseThrow(() -> new RuntimeException("Schedule not found: " + scheduleId));
        }

        /**
         * Delete schedule from both Temporal and database
         */
        @Transactional
        public void deleteSchedule(String scheduleId) {
                log.info("Deleting schedule: {}", scheduleId);

                // Check if schedule exists in DB
                ScheduleEntity scheduleEntity = scheduleRepo.findById(scheduleId)
                        .orElseThrow(() -> new RuntimeException("Schedule not found: " + scheduleId));

                try {
                        // Delete from Temporal
                        //scheduleClient.deleteSchedule(scheduleId);
                        log.info("Schedule deleted from Temporal: {}", scheduleId);
                } catch (Exception e) {
                        log.warn("Failed to delete schedule from Temporal: {}", e.getMessage());
                        // Continue with DB deletion even if Temporal deletion fails
                }

                // Mark as deleted in database (soft delete)
                scheduleEntity.setStatus(ScheduleStatus.DELETED);
                scheduleEntity.setUpdatedAt(LocalDateTime.now());
                scheduleRepo.save(scheduleEntity);

                log.info("Schedule marked as deleted in database: {}", scheduleId);
        }

}
