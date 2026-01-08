package com.ngoctran.interactionservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller to simulate external system callbacks
 * Used for testing async flows without real 3rd party services
 */
@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
@Slf4j
public class SimulationController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.ngoctran.interactionservice.engine.OnboardingEngine onboardingEngine;
    private final com.ngoctran.interactionservice.repo.OnboardingInstanceRepository instanceRepository;

    @PostMapping("/ekyc-callback")
    public String simulateEkyc(@RequestParam UUID instanceId,
            @RequestParam(defaultValue = "APPROVED") String result,
            @RequestParam(defaultValue = "0.95") Double score) {

        String eventId = UUID.randomUUID().toString();

        // Auto-fix: If user is still at DOC_UPLOADED, move them to EKYC_PENDING first
        try {
            instanceRepository.findById(instanceId).ifPresent(i -> {
                if ("DOC_UPLOADED".equals(i.getCurrentState())) {
                    log.info("User still at DOC_UPLOADED, performing NEXT for them...");
                    onboardingEngine.handle(com.ngoctran.interactionservice.engine.ActionCommand.builder()
                            .instanceId(instanceId)
                            .action("NEXT")
                            .actor("SYSTEM")
                            .requestId(UUID.randomUUID().toString())
                            .build());
                }
            });
        } catch (Exception e) {
            log.warn("Auto-fix transition failed: {}", e.getMessage());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("result", result);
        payload.put("score", score);

        Map<String, Object> correlation = new HashMap<>();
        correlation.put("instanceId", instanceId.toString());
        payload.put("correlation", correlation);

        log.info("Simulating eKYC callback for instance: {}", instanceId);
        try {
            kafkaTemplate.send("ekyc-callback", instanceId.toString(), payload).get(1,
                    java.util.concurrent.TimeUnit.SECONDS);
            return "Simulated eKYC callback sent to Kafka for " + instanceId;
        } catch (Exception e) {
            log.warn("Kafka send failed, falling back to direct engine call: {}", e.getMessage());

            // Fallback logic
            String action = "APPROVED".equalsIgnoreCase(result) || "OK".equalsIgnoreCase(result) ? "EKYC_CALLBACK_OK"
                    : "EKYC_CALLBACK_FAIL";
            com.ngoctran.interactionservice.engine.ActionCommand command = com.ngoctran.interactionservice.engine.ActionCommand
                    .kafka(instanceId, action, eventId);
            onboardingEngine.handle(command);

            return "Simulated eKYC callback processed directly (Kafka unavailable) for " + instanceId;
        }
    }

    @PostMapping("/aml-callback")
    public String simulateAml(@RequestParam UUID instanceId,
            @RequestParam(defaultValue = "CLEAR") String result) {

        String eventId = UUID.randomUUID().toString();

        // Auto-fix: If user is still at EKYC_APPROVED, move them to AML_PENDING first
        try {
            instanceRepository.findById(instanceId).ifPresent(i -> {
                if ("EKYC_APPROVED".equals(i.getCurrentState())) {
                    log.info("User still at EKYC_APPROVED, performing NEXT for them...");
                    onboardingEngine.handle(com.ngoctran.interactionservice.engine.ActionCommand.builder()
                            .instanceId(instanceId)
                            .action("NEXT")
                            .actor("SYSTEM")
                            .requestId(UUID.randomUUID().toString())
                            .build());
                }
            });
        } catch (Exception e) {
            /* ignore */ }

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("result", result);

        Map<String, Object> correlation = new HashMap<>();
        correlation.put("instanceId", instanceId.toString());
        payload.put("correlation", correlation);

        log.info("Simulating AML callback for instance: {}", instanceId);
        try {
            kafkaTemplate.send("aml-callback", instanceId.toString(), payload).get(1,
                    java.util.concurrent.TimeUnit.SECONDS);
            return "Simulated AML callback sent to Kafka for " + instanceId;
        } catch (Exception e) {
            log.warn("Kafka send failed, falling back to direct engine call: {}", e.getMessage());

            // Fallback logic
            String action = "CLEAR".equalsIgnoreCase(result) || "OK".equalsIgnoreCase(result)
                    || "PASS".equalsIgnoreCase(result) ? "AML_CALLBACK_OK" : "AML_CALLBACK_FAIL";
            com.ngoctran.interactionservice.engine.ActionCommand command = com.ngoctran.interactionservice.engine.ActionCommand
                    .kafka(instanceId, action, eventId);
            onboardingEngine.handle(command);

            return "Simulated AML callback processed directly (Kafka unavailable) for " + instanceId;
        }
    }
}
