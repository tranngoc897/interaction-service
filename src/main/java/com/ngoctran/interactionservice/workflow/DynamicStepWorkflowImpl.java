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

            if (activityName == null) {
                log.warn("Action config missing 'activity' name, skipping: {}", actionConfig);
                continue;
            }

            log.info("Dynamically calling activity: {} for case: {}", activityName, caseId);

            // 3. Invoke Activity Dynamically
            // The activity is identified by its @ActivityMethod name OR the generic name if
            // registered that way
            try {
                // Pass current context (data) to the activity
                // We expect activities to return a Map of results
                Map<String, Object> activityResult = untypedActivity.execute(
                        activityName,
                        Map.class,
                        caseId,
                        currentContext);

                // 4. Merge results into context for the NEXT activity to use
                if (activityResult != null) {
                    log.info("Activity {} returned results. Merging into context.", activityName);
                    currentContext.putAll(activityResult);
                }

            } catch (Exception e) {
                log.error("Dynamic activity {} failed", activityName, e);
                // Based on business logic, we could throw exception to fail workflow or
                // continue
                throw e;
            }
        }

        log.info("All dynamic actions completed for case: {}", caseId);
        return currentContext;
    }
}
