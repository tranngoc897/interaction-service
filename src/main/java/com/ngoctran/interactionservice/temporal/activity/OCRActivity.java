package com.ngoctran.interactionservice.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.Map;

@ActivityInterface
public interface OCRActivity {
    
    @ActivityMethod
    OCRResult extractText(String documentUrl, String documentType);
    
    class OCRResult {
        private boolean success;
        private Map<String, Object> extractedData;
        private double confidence;
        private String errorMessage;
        
        public OCRResult() {}
        
        public OCRResult(boolean success, Map<String, Object> extractedData, double confidence) {
            this.success = success;
            this.extractedData = extractedData;
            this.confidence = confidence;
        }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public Map<String, Object> getExtractedData() { return extractedData; }
        public void setExtractedData(Map<String, Object> extractedData) { this.extractedData = extractedData; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
