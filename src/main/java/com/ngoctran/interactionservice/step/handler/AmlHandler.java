package com.ngoctran.interactionservice.step.handler;

import com.ngoctran.interactionservice.step.StepHandler;
import com.ngoctran.interactionservice.step.StepResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component("AML_PENDING")
@RequiredArgsConstructor
public class AmlHandler implements StepHandler {

        private final KafkaTemplate<String, Object> kafkaTemplate;

        @Override
        @CircuitBreaker(name = "aml-service")
        public StepResult execute(UUID instanceId) {
                log.info("Executing AML check for instance: {}", instanceId);

                try {
                        // Publish AML request to Kafka
                        kafkaTemplate.send("aml-request", instanceId.toString(),
                                        createAmlRequest(instanceId));

                        log.info("AML request published to Kafka for instance: {}", instanceId);

                        // For async steps, we return success immediately
                        // The actual result will come via callback
                        return StepResult.success();

                } catch (Exception ex) {
                        log.error("Error publishing AML request for instance: {}", instanceId, ex);
                        return StepResult.failure(
                                        new com.ngoctran.interactionservice.step.StepError(
                                                        "AML_REQUEST_FAILED",
                                                        com.ngoctran.interactionservice.step.ErrorType.TRANSIENT,
                                                        "Failed to publish AML request: " + ex.getMessage()));
                }
        }

        private Object createAmlRequest(UUID instanceId) {
                // In real implementation, this would gather customer data from database
                return Map.of(
                                "eventId", UUID.randomUUID().toString(),
                                "eventType", "AML_REQUEST",
                                "correlation", Map.of(
                                                "instanceId", instanceId.toString(),
                                                "step", "AML_PENDING"),
                                "payload", Map.of(
                                                "instanceId", instanceId.toString(),
                                                "checkType", "FULL_SCREENING",
                                                "includeSanctions", true,
                                                "includePep", true));
        }
}
