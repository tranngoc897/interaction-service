package com.ngoctran.interactionservice.step.handler;

import com.ngoctran.interactionservice.step.StepHandler;
import com.ngoctran.interactionservice.step.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Milestones Handlers for states that just represent progress
 * but don't have complex business logic.
 */
@Slf4j
@Configuration
public class MilestoneHandlers {

    @Component("EKYC_APPROVED")
    public static class EkycApprovedHandler implements StepHandler {
        @Override
        public StepResult execute(UUID instanceId) {
            log.info("eKYC Approved milestone reached for instance: {}", instanceId);
            return StepResult.success();
        }
    }

    @Component("AML_CLEARED")
    public static class AmlClearedHandler implements StepHandler {
        @Override
        public StepResult execute(UUID instanceId) {
            log.info("AML Cleared milestone reached for instance: {}", instanceId);
            return StepResult.success();
        }
    }

    @Component("COMPLETED")
    public static class CompletedHandler implements StepHandler {
        @Override
        public StepResult execute(UUID instanceId) {
            log.info("Onboarding Completed for instance: {}", instanceId);
            return StepResult.success();
        }
    }

    @Component("EKYC_REJECTED")
    public static class EkycRejectedHandler implements StepHandler {
        @Override
        public StepResult execute(UUID instanceId) {
            log.warn("eKYC Rejected for instance: {}", instanceId);
            return StepResult.success();
        }
    }

    @Component("AML_REJECTED")
    public static class AmlRejectedHandler implements StepHandler {
        @Override
        public StepResult execute(UUID instanceId) {
            log.warn("AML Rejected for instance: {}", instanceId);
            return StepResult.success();
        }
    }

    @Component("TIMEOUT")
    public static class TimeoutHandler implements StepHandler {
        @Override
        public StepResult execute(UUID instanceId) {
            log.error("Workflow Timeout for instance: {}", instanceId);
            return StepResult.success();
        }
    }
}
