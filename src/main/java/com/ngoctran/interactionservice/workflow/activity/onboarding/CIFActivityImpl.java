package com.ngoctran.interactionservice.workflow.activity.onboarding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CIFActivityImpl implements CIFActivity {

    @Override
    public CIFResult createCIF(String customerId, String caseId, CIFData cifData) {
        log.info("Creating CIF for customer: {}, case: {}", customerId, caseId);

        try {
            Thread.sleep(1000 + (int)(Math.random() * 2000)); // 1-3 seconds

            String cifNumber = "CIF" + String.format("%010d", System.nanoTime() % 10000000000L);
            String coreCustomerId = "CORE" + String.format("%012d", System.nanoTime() % 1000000000000L);

            boolean success = Math.random() > 0.05;

            if (success) {
                log.info("CIF created successfully: cifNumber={}, coreCustomerId={}", cifNumber, coreCustomerId);
                return new CIFResult(true, cifNumber, coreCustomerId, null, System.currentTimeMillis());
            } else {
                String errorMessage = "Core banking system temporarily unavailable";
                log.error("CIF creation failed: {}", errorMessage);
                return new CIFResult(false, null, null, errorMessage, System.currentTimeMillis());
            }

        } catch (Exception e) {
            log.error("CIF creation failed for customer: {}", customerId, e);
            return new CIFResult(false, null, null, "CIF creation error: " + e.getMessage(), System.currentTimeMillis());
        }
    }
}
