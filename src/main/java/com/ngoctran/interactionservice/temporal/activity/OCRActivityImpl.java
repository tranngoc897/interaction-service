package com.ngoctran.interactionservice.temporal.activity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class OCRActivityImpl implements OCRActivity {
    
    @Override
    public OCRResult extractText(String documentUrl, String documentType) {
        log.info("Extracting text from document: type={}, url={}", documentType, documentUrl);
        
        try {
            // TODO: Integrate with actual OCR service (Google Vision, AWS Textract, etc.)
            // For now, return mock data
            
            Map<String, Object> extractedData = new HashMap<>();
            
            switch (documentType) {
                case "id-front":
                    extractedData.put("idNumber", "123456789");
                    extractedData.put("fullName", "NGUYEN VAN A");
                    extractedData.put("dob", "01/01/1990");
                    extractedData.put("address", "123 Nguyen Hue, HCM");
                    break;
                    
                case "id-back":
                    extractedData.put("issueDate", "01/01/2020");
                    extractedData.put("expiryDate", "01/01/2030");
                    extractedData.put("placeOfOrigin", "Ho Chi Minh");
                    break;
                    
                default:
                    extractedData.put("documentType", documentType);
            }
            
            log.info("OCR extraction successful: {}", extractedData);
            return new OCRResult(true, extractedData, 0.95);
            
        } catch (Exception e) {
            log.error("OCR extraction failed", e);
            OCRResult result = new OCRResult();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }
}
