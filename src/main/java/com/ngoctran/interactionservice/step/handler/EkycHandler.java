package com.ngoctran.interactionservice.step.handler;

import com.ngoctran.interactionservice.step.StepHandler;
import com.ngoctran.interactionservice.step.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Slf4j
@Component("EKYC_PENDING")
@RequiredArgsConstructor
public class EkycHandler implements StepHandler {

        private final KafkaTemplate<String, Object> kafkaTemplate;

        @Override
        @CircuitBreaker(name = "ekyc-service")
        public StepResult execute(UUID instanceId) {
                log.info("Executing eKYC request for instance: {}", instanceId);

                try {
                        // Publish eKYC request to Kafka
                        // In real implementation, this would include document data
                        kafkaTemplate.send("ekyc-request", instanceId.toString(),
                                        createEkycRequest(instanceId));

                        log.info("eKYC request published to Kafka for instance: {}", instanceId);

                        // For async steps, we return success immediately
                        // The actual result will come via callback
                        return StepResult.success();

                } catch (Exception ex) {
                        log.error("Error publishing eKYC request for instance: {}", instanceId, ex);
                        return StepResult.failure(
                                        new com.ngoctran.interactionservice.step.StepError(
                                                        "EKYC_REQUEST_FAILED",
                                                        com.ngoctran.interactionservice.step.ErrorType.TRANSIENT,
                                                        "Failed to publish eKYC request: " + ex.getMessage()));
                }
        }

        private Object createEkycRequest(UUID instanceId) {
                // In real implementation, this would gather document data from database
                return Map.of(
                                "eventId", UUID.randomUUID().toString(),
                                "eventType", "EKYC_REQUEST",
                                "correlation", Map.of(
                                                "instanceId", instanceId.toString(),
                                                "step", "EKYC_PENDING"),
                                "payload", Map.of(
                                                "instanceId", instanceId.toString(),
                                                "requestType", "FULL_VERIFICATION"));
        }
}
