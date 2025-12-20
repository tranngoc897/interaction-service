package com.ngoctran.interactionservice.temporal.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.cases.CaseEntity;
import com.ngoctran.interactionservice.cases.CaseRepository;
import com.ngoctran.interactionservice.interaction.InteractionEntity;
import com.ngoctran.interactionservice.interaction.InteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InteractionCallbackActivityImpl implements InteractionCallbackActivity {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InteractionCallbackActivityImpl.class);
    
    private final InteractionRepository interactionRepo;
    private final CaseRepository caseRepo;
    private final ObjectMapper objectMapper;
    
    @Override
    public void updateInteractionStatus(
            String interactionId,
            String status,
            String reason,
            Map<String, Object> data) {
        
        log.info("Updating interaction status: id={}, status={}", interactionId, status);
        
        try {
            InteractionEntity interaction = interactionRepo.findById(interactionId)
                    .orElseThrow(() -> new RuntimeException("Interaction not found: " + interactionId));
            
            interaction.setStatus(status);
            
            // Update temp data with workflow result
            if (data != null) {
                String jsonData = objectMapper.writeValueAsString(data);
                interaction.setTempData(jsonData);
            }
            
            interactionRepo.save(interaction);
            
            log.info("Interaction status updated successfully");
            
        } catch (Exception e) {
            log.error("Failed to update interaction status", e);
            throw new RuntimeException("Failed to update interaction", e);
        }
    }
    
    @Override
    public void updateCaseData(String caseId, Map<String, Object> data) {
        log.info("Updating case data: caseId={}", caseId);
        
        try {
            CaseEntity caseEntity = caseRepo.findById(UUID.fromString(caseId))
                    .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
            
            // Merge new data with existing case data
            String existingData = caseEntity.getCaseData();
            Map<String, Object> existingMap = existingData != null && !existingData.isEmpty()
                    ? objectMapper.readValue(existingData, Map.class)
                    : new HashMap<>();
            
            Map<String, Object> mergedData = new HashMap<>(existingMap);
            mergedData.putAll(data);
            
            String jsonData = objectMapper.writeValueAsString(mergedData);
            caseEntity.setCaseData(jsonData);
            
            caseRepo.save(caseEntity);
            
            log.info("Case data updated successfully");
            
        } catch (Exception e) {
            log.error("Failed to update case data", e);
            throw new RuntimeException("Failed to update case", e);
        }
    }
}
