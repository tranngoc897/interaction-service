package com.ngoctran.interactionservice.kafka;

import com.ngoctran.interactionservice.engine.ActionCommand;
import com.ngoctran.interactionservice.engine.OnboardingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmlCallbackConsumer {

    private final OnboardingEngine onboardingEngine;
    private final com.ngoctran.interactionservice.repo.StateContextRepository stateContextRepository;
    private final com.ngoctran.interactionservice.repo.OnboardingInstanceRepository instanceRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(topics = "aml-callback", groupId = "onboarding-service")
    public void consumeAmlCallback(@Payload Map<String, Object> event, Acknowledgment ack) {
        try {
            log.info("Received AML callback: {}", event);

            String eventId = (String) event.get("eventId");
            String instanceIdStr = extractInstanceId(event);
            String result = (String) event.get("result");

            if (instanceIdStr == null || result == null) {
                log.error("Invalid AML callback event: missing instanceId or result");
                ack.acknowledge();
                return;
            }

            UUID instanceId = UUID.fromString(instanceIdStr);

            // Auto-fix: If user is stuck in EKYC_APPROVED, move them to AML_PENDING first
            try {
                instanceRepository.findById(instanceId).ifPresent(i -> {
                    if ("EKYC_APPROVED".equals(i.getCurrentState())) {
                        log.info("Consumer detected instance {} at EKYC_APPROVED, performing NEXT auto-fix...",
                                instanceId);
                        onboardingEngine.handle(ActionCommand.builder()
                                .instanceId(instanceId)
                                .action("NEXT")
                                .actor("SYSTEM")
                                .requestId(UUID.randomUUID().toString())
                                .build());
                    }
                });
            } catch (Exception e) {
                log.warn("Auto-fix from consumer failed: {}", e.getMessage());
            }

            // Save context data (aml status)
            saveContextData(instanceId, result);

            // Determine action based on result
            String action = mapResultToAction(result);

            // Create action command
            ActionCommand command = ActionCommand.kafka(instanceId, action, eventId);

            // Process through engine
            onboardingEngine.handle(command);

            log.info("Processed AML callback for instance {} with result {}", instanceId, result);
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Error processing AML callback: {}", event, ex);
            // In production, you might want to send to DLQ here
            ack.acknowledge();
        }
    }

    private String extractInstanceId(Map<String, Object> event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> correlation = (Map<String, Object>) event.get("correlation");
            if (correlation != null) {
                return (String) correlation.get("instanceId");
            }
        } catch (Exception ex) {
            log.warn("Could not extract instanceId from correlation", ex);
        }
        return null;
    }

    private String mapResultToAction(String result) {
        switch (result.toUpperCase()) {
            case "CLEAR":
            case "OK":
            case "PASS":
                return "AML_CALLBACK_OK";
            case "HIT":
            case "FAIL":
            case "REJECTED":
                return "AML_CALLBACK_FAIL";
            default:
                log.warn("Unknown AML result: {}, defaulting to FAIL", result);
                return "AML_CALLBACK_FAIL";
        }
    }

    private void saveContextData(UUID instanceId, String result) {
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

            // Map result to a consistent status for RuleEngine
            if ("CLEAR".equalsIgnoreCase(result) || "OK".equalsIgnoreCase(result) || "PASS".equalsIgnoreCase(result)) {
                data.put("aml_status", "CLEAR");
            } else {
                data.put("aml_status", "REJECTED");
            }

            context.setContextData(objectMapper.writeValueAsString(data));
            context.setUpdatedAt(java.time.Instant.now());

            stateContextRepository.save(context);
            log.info("Updated context with aml_status for instance: {}", instanceId);

        } catch (Exception e) {
            log.error("Failed to save context for instance: {}", instanceId, e);
        }
    }
}
