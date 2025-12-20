package com.ngoctran.interactionservice.interaction.service;

import com.ngoctran.interactionservice.interaction.InteractionRepository;
import org.springframework.stereotype.Service;

@Service
public class InteractionUpdateService {

    private final InteractionRepository repo;

    public InteractionUpdateService(InteractionRepository repo) {
        this.repo = repo;
    }

    public void markStepStarted(String interactionId, String stepName) {
        repo.findById(interactionId).ifPresent(interaction -> {
            interaction.setStepName(stepName);
            interaction.setStepStatus("PENDING");
            repo.save(interaction);
        });
    }

    public void markStepCompleted(String interactionId, String stepName, String resultJson) {
        repo.findById(interactionId).ifPresent(interaction -> {
            interaction.setStepName(stepName);
            interaction.setStepStatus("COMPLETED");
            interaction.setTempData(resultJson);
            repo.save(interaction);
        });
    }

    public void markWorkflowCompleted(String interactionId) {
        repo.findById(interactionId).ifPresent(interaction -> {
            interaction.setStatus("COMPLETED");
            repo.save(interaction);
        });
    }
}
