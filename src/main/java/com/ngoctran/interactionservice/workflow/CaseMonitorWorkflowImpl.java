package com.ngoctran.interactionservice.workflow;

import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class CaseMonitorWorkflowImpl implements CaseMonitorWorkflow {

    private static final Logger log = Workflow.getLogger(CaseMonitorWorkflowImpl.class);
    private static final int CONTINUE_AS_NEW_THRESHOLD = 10; // Low for demo, usually ~1000s

    private String lastStatus = "STARTED";
    private int eventCount = 0;
    private boolean exit = false;

    @Override
    public void monitorCase(String caseId, int iterationCount) {
        log.info("Monitoring Case: {} | Iteration: {} | Current History Size: {}",
                caseId, iterationCount, Workflow.getInfo().getHistorySize());

        // Event loop
        while (!exit) {
            // Wait for a signal or a timeout
            Workflow.await(Duration.ofDays(1), () -> eventCount >= CONTINUE_AS_NEW_THRESHOLD || exit);

            if (exit) {
                log.info("Case Monitor for {} exiting", caseId);
                break;
            }

            // CRITICAL: ContinueAsNew Logic
            // When history grows too large, we restart the workflow with fresh history but
            // same status
            if (eventCount >= CONTINUE_AS_NEW_THRESHOLD) {
                log.info("History threshold reached for {}. Continuing as New...", caseId);

                // This resets the history but starts the method again with these parameters
                Workflow.continueAsNew(caseId, iterationCount + 1);

                // Code after continueAsNew is never executed
                return;
            }
        }
    }

    @Override
    public void updateStatus(String status) {
        this.lastStatus = status;
        this.eventCount++;
        log.info("Received status update: {}. Total events in this iteration: {}", status, eventCount);
    }
}
