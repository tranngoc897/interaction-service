package com.ngoctran.interactionservice.cases;


import com.ngoctran.interactionservice.FlowDefinitionLoader;
import com.ngoctran.interactionservice.KafkaPublisher;
import com.ngoctran.interactionservice.NextStepResponse;
import com.ngoctran.interactionservice.StepSubmissionDto;
import com.ngoctran.interactionservice.WorkflowClientWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class CaseService {
    private final CaseRepository repo;
    private final FlowDefinitionLoader flowLoader;
    private final WorkflowClientWrapper workflowClient;
    private final KafkaPublisher kafka;

    public CaseService(CaseRepository repo, FlowDefinitionLoader flowLoader, WorkflowClientWrapper workflowClient, KafkaPublisher kafka) {
        this.repo = repo;
        this.flowLoader = flowLoader;
        this.workflowClient = workflowClient;
        this.kafka = kafka;
    }

    @Transactional
    public UUID createCase(Map<String,Object> initialData) {
        UUID caseId = UUID.randomUUID();
        CaseEntity c = new CaseEntity();
        c.setCaseId(caseId);
        c.setStatus("IN_PROGRESS");
        c.setCurrentStep("upload-docs");
        c.setCaseData(initialData == null ? "{}" : toJson(initialData));
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        repo.save(c);
        kafka.publishCaseEvent(caseId, "CASE_CREATED", Map.of("initialData", initialData));
        return caseId;
    }

    public Map<String,Object> getCase(UUID caseId) {
        return Map.of("case", repo.findById(caseId).orElse(null));
    }

    @Transactional
    public NextStepResponse submitStep(UUID caseId, StepSubmissionDto submission) {
        CaseEntity c = repo.findById(caseId).orElseThrow();
        // persist step data into steps JSONB (merge) - simplified here
        String stepsJson = c.getSteps(); // parse merge...
        // update caseData
        c.setCaseData(mergeJson(c.getCaseData(), submission.stepData()));
        c.setUpdatedAt(Instant.now());
        // decide next action by reading flow definition
        Map<String,Object> flow = loadFlowConfig(c); // implement load
        Map<String,Object> stepDef = findStep(flow, submission.stepName());
        // handle actions
        List<Map<String,Object>> actions = (List<Map<String,Object>>)stepDef.get("onSubmit");
        for (Map<String,Object> action : actions) {
            String act = (String)action.get("action");
            if ("startWorkflow".equals(act) || "start_workflow".equals(act)) {
                String wfName = (String)action.get("workflow");
                String wfInstanceId = workflowClient.startOnboardingWorkflow(caseId.toString(), Map.of("caseId", caseId.toString(), "stepData", submission.stepData()));
                c.setWorkflowInstanceId(wfInstanceId);
                c.setStatus("WAITING_SYSTEM");
            } else if ("callService".equals(act)) {
                // call short sync service or publish event
            } else if ("persistToCaseStore".equals(act)) {
                // already persisted
            }
        }
        repo.save(c);
        kafka.publishCaseEvent(caseId, "STEP_SUBMITTED", Map.of("step", submission.stepName()));
        // determine nextStep to return to client (could be read from flow or from WF status)
        String nextStep = determineNextStep(flow, submission.stepName());
        return new NextStepResponse(nextStep, Map.of(), c.getStatus());
    }

    // helpers: toJson, mergeJson, loadFlowConfig, findStep, determineNextStep...
}