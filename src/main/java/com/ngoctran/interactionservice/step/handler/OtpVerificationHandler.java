package com.ngoctran.interactionservice.step.handler;

import com.ngoctran.interactionservice.step.StepHandler;
import com.ngoctran.interactionservice.step.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component("OTP_VERIFIED")
public class OtpVerificationHandler implements StepHandler {

    @Override
    public StepResult execute(UUID instanceId) {
        log.info("Executing OTP verification for instance: {}", instanceId);

        try {
            // In real implementation, this would:
            // 1. Call OTP service to verify code
            // 2. Check if OTP is valid and not expired
            // 3. Return success/failure based on verification result

            // For now, simulate success
            boolean otpValid = simulateOtpVerification(instanceId);

            if (otpValid) {
                log.info("OTP verification successful for instance: {}", instanceId);
                return StepResult.success();
            } else {
                log.warn("OTP verification failed for instance: {}", instanceId);
                return StepResult.failure(
                        new com.ngoctran.interactionservice.step.StepError(
                                "OTP_INVALID",
                                com.ngoctran.interactionservice.step.ErrorType.BUSINESS,
                                "Invalid or expired OTP"
                        )
                );
            }

        } catch (Exception ex) {
            log.error("Error during OTP verification for instance: {}", instanceId, ex);
            return StepResult.failure(
                    new com.ngoctran.interactionservice.step.StepError(
                            "OTP_VERIFICATION_ERROR",
                            com.ngoctran.interactionservice.step.ErrorType.SYSTEM,
                            "OTP verification service error: " + ex.getMessage()
                    )
            );
        }
    }

    private boolean simulateOtpVerification(UUID instanceId) {
        // Simulate OTP verification logic
        // In production, this would call an external OTP service
        return true; // Assume success for demo
    }
}
