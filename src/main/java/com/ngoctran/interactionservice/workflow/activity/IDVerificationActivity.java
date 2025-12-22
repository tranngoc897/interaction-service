package com.ngoctran.interactionservice.workflow.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface IDVerificationActivity {
    
    @ActivityMethod
    IDVerificationResult verifyID(String idNumber, String fullName, String dob, String selfieUrl);
    
    class IDVerificationResult {
        private boolean verified;
        private double confidenceScore;
        private double faceMatchScore;
        private String reason;
        
        public IDVerificationResult() {}
        
        public IDVerificationResult(boolean verified, double confidenceScore, double faceMatchScore) {
            this.verified = verified;
            this.confidenceScore = confidenceScore;
            this.faceMatchScore = faceMatchScore;
        }
        
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
        public double getFaceMatchScore() { return faceMatchScore; }
        public void setFaceMatchScore(double faceMatchScore) { this.faceMatchScore = faceMatchScore; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
