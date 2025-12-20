package com.ngoctran.interactionservice.temporal.activity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
public class IDVerificationActivityImpl implements IDVerificationActivity {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IDVerificationActivityImpl.class);
    
    @Override
    public IDVerificationResult verifyID(String idNumber, String fullName, String dob, String selfieUrl) {
        log.info("Verifying ID: idNumber={}, fullName={}, dob={}", idNumber, fullName, dob);
        
        try {
            // TODO: Integrate with actual ID verification service
            // Examples: Jumio, Onfido, Trulioo, etc.
            
            // Mock verification logic
            boolean isValid = idNumber != null && idNumber.length() >= 9;
            double confidenceScore = isValid ? 0.92 : 0.45;
            double faceMatchScore = 0.88;
            
            IDVerificationResult result = new IDVerificationResult(isValid, confidenceScore, faceMatchScore);
            result.setReason(isValid ? "ID verified successfully" : "Invalid ID number");
            
            log.info("ID verification completed: verified={}, confidence={}", isValid, confidenceScore);
            return result;
            
        } catch (Exception e) {
            log.error("ID verification failed", e);
            IDVerificationResult result = new IDVerificationResult();
            result.setVerified(false);
            result.setReason("Verification service error: " + e.getMessage());
            return result;
        }
    }
}
