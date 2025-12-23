package com.ngoctran.interactionservice.workflow;

import com.ngoctran.interactionservice.workflow.activity.CleanupActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class CleanupWorkflowImpl implements CleanupWorkflow {

    private final Logger log = Workflow.getLogger(CleanupWorkflowImpl.class);

    private final CleanupActivity cleanupActivity = Workflow.newActivityStub(
            CleanupActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(10))
                    .build());

    @Override
    public void runCleanup() {
        log.info("Executing scheduled cleanup workflow");

        // Cleanup interactions older than 24 hours
        int count = cleanupActivity.cleanupstaleInteractions(24);

        log.info("Cleanup completed. Total interactions cancelled: {}", count);
    }
}
