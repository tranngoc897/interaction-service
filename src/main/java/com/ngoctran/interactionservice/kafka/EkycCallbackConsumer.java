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
public class EkycCallbackConsumer {

    private final OnboardingEngine onboardingEngine;

    @KafkaListener(topics = "ekyc-callback", groupId = "onboarding-service")
    public void consumeEkycCallback(@Payload Map<String, Object> event, Acknowledgment ack) {
        try {
            log.info("Received eKYC callback: {}", event);

            String eventId = (String) event.get("eventId");
            String instanceIdStr = extractInstanceId(event);
            String result = (String) event.get("result");

            if (instanceIdStr == null || result == null) {
                log.error("Invalid eKYC callback event: missing instanceId or result");
                ack.acknowledge();
                return;
            }

            UUID instanceId = UUID.fromString(instanceIdStr);

            // Determine action based on result
            String action = mapResultToAction(result);

            // Create action command
            ActionCommand command = ActionCommand.kafka(instanceId, action, eventId);

            // Process through engine
            onboardingEngine.handle(command);

            log.info("Processed eKYC callback for instance {} with result {}", instanceId, result);
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Error processing eKYC callback: {}", event, ex);
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
            case "APPROVED":
            case "OK":
                return "EKYC_CALLBACK_OK";
            case "REJECTED":
            case "FAIL":
                return "EKYC_CALLBACK_FAIL";
            default:
                log.warn("Unknown eKYC result: {}, defaulting to FAIL", result);
                return "EKYC_CALLBACK_FAIL";
        }
    }
}
