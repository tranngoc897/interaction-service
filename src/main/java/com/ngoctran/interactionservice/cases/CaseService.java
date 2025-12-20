package com.ngoctran.interactionservice.cases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.NextStepResponse;
import com.ngoctran.interactionservice.StepSubmissionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final CaseRepository caseRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID createCase(Map<String, Object> initialData) {
        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setStatus("ACTIVE");
        if (initialData != null) {
            caseEntity.setCaseData(toJson(initialData));
        }
        caseEntity = caseRepo.save(caseEntity);
        log.info("Created new case with ID: {}", caseEntity.getId());
        return caseEntity.getId();
    }

    @Transactional(readOnly = true)
    public CaseEntity getCase(UUID caseId) {
        return caseRepo.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
    }

    @Transactional
    public NextStepResponse submitStep(UUID caseId, StepSubmissionDto submission) {
        log.info("Submitting step for case {}: {}", caseId, submission.stepName());
        // This is a placeholder for direct case step submission
        return new NextStepResponse(null, Map.of(), "COMPLETED");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}