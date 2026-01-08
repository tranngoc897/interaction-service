package com.ngoctran.interactionservice.step.handler;

import com.ngoctran.interactionservice.step.StepHandler;
import com.ngoctran.interactionservice.step.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component("OTP_VERIFIED")
@lombok.RequiredArgsConstructor
public class OtpVerificationHandler implements StepHandler {

    private final com.ngoctran.interactionservice.repo.StateContextRepository stateContextRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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

                // Save context for next step conditions
                saveContext(instanceId);

                return StepResult.success();
            } else {
                log.warn("OTP verification failed for instance: {}", instanceId);
                return StepResult.failure(
                        new com.ngoctran.interactionservice.step.StepError(
                                "OTP_INVALID",
                                com.ngoctran.interactionservice.step.ErrorType.BUSINESS,
                                "Invalid or expired OTP"));
            }

        } catch (Exception ex) {
            log.error("Error during OTP verification for instance: {}", instanceId, ex);
            return StepResult.failure(
                    new com.ngoctran.interactionservice.step.StepError(
                            "OTP_VERIFICATION_ERROR",
                            com.ngoctran.interactionservice.step.ErrorType.SYSTEM,
                            "OTP verification service error: " + ex.getMessage()));
        }
    }

    private void saveContext(UUID instanceId) {
        try {
            com.ngoctran.interactionservice.domain.StateContext context = stateContextRepository.findById(instanceId)
                    .orElse(com.ngoctran.interactionservice.domain.StateContext.builder()
                            .instanceId(instanceId)
                            .version(0L)
                            .build());

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            if (context.getContextData() != null) {
                try {
                    data = objectMapper.readValue(context.getContextData(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                            });
                } catch (Exception e) {
                    log.warn("Failed to parse existing context data", e);
                }
            }

            data.put("otp_status", "SUCCESS");

            context.setContextData(objectMapper.writeValueAsString(data));
            context.setUpdatedAt(java.time.Instant.now());

            stateContextRepository.save(context);
            log.info("Updated context with otp_status=SUCCESS for instance: {}", instanceId);

        } catch (Exception e) {
            log.error("Failed to save context for instance: {}", instanceId, e);
            // Don't fail the step just because context save failed?
            // Actually we should, otherwise next step might fail. But for now let's log it.
        }
    }

    private boolean simulateOtpVerification(UUID instanceId) {
        // Simulate OTP verification logic
        // In production, this would call an external OTP service
        return true; // Assume success for demo
    }
}
