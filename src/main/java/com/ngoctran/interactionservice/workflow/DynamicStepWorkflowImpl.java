package com.ngoctran.interactionservice.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamicStepWorkflowImpl implements DynamicStepWorkflow {

    private static final Logger log = Workflow.getLogger(DynamicStepWorkflowImpl.class);

    @Override
    public Map<String, Object> executeStepActions(String caseId, List<Map<String, Object>> actions,
            Map<String, Object> initialData) {
        log.info("Executing dynamic actions for case: {}", caseId);

        // currentContext will store shared data between activities
        Map<String, Object> currentContext = new HashMap<>(initialData);

        // 1. Create Untyped Stub
        ActivityStub untypedActivity = Workflow.newUntypedActivityStub(
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofMinutes(5))
                        .build());

        // 2. Iterate through actions defined in Blueprint (onSubmit)
        for (Map<String, Object> actionConfig : actions) {
            String activityName = (String) actionConfig.get("activity");
            String requiredField = (String) actionConfig.get("runIfFieldExists");
            if (activityName == null) {
                log.warn("Action config missing 'activity' name, skipping: {}", actionConfig);
                continue;
            }
            // 1. CONDITION CHECK: If-Else logic based on data
            if (requiredField != null && !currentContext.containsKey(requiredField)) {
                log.info("Skipping activity {} because required field {} is missing from context",
                        activityName, requiredField);
                continue;
            }
            log.info("Dynamically calling activity: {} for case: {}", activityName, caseId);
            try {
                // 2. TRIGGER ACTIVITY:
                Map<String, Object> activityResult = untypedActivity.execute(
                        activityName,
                        Map.class,
                        caseId,
                        currentContext);
                // 3. DATA PASSING: Merge results back to context
                if (activityResult != null) {
                    currentContext.putAll(activityResult);
                }
                // 4. DYNAMIC NEW TRIGGER: Example of triggering a NEW activity
                // based on the result of the previous one
                if (Boolean.TRUE.equals(currentContext.get("needsManualReview"))) {
                    log.warn("Flag 'needsManualReview' detected! Triggering ReviewActivity on the fly.");
                    untypedActivity.execute("CreateManualTaskActivity", Void.class, caseId, "Auto-triggered review");
                }

            } catch (Exception e) {
                log.error("Dynamic activity {} failed", activityName, e);
                throw e;
            }
        }

        log.info("All dynamic actions completed for case: {}", caseId);
        return currentContext;
    }
}
