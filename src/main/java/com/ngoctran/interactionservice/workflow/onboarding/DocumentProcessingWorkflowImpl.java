package com.ngoctran.interactionservice.workflow.onboarding;

import com.ngoctran.interactionservice.workflow.activity.IDVerificationActivity;
import com.ngoctran.interactionservice.workflow.activity.OCRActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DocumentProcessingWorkflowImpl implements DocumentProcessingWorkflow {

    private final Logger log = Workflow.getLogger(DocumentProcessingWorkflowImpl.class);

    private final OCRActivity ocrActivity = Workflow.newActivityStub(
            OCRActivity.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(2)).build());

    private final IDVerificationActivity idVerificationActivity = Workflow.newActivityStub(
            IDVerificationActivity.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(2)).build());

    @Override
    public Map<String, Object> processDocuments(String caseId, Map<String, Object> initialData) {
        log.info("Starting Child Workflow: DocumentProcessing for case: {}", caseId);

        // 1. Mock document URLs
        Map<String, String> mockDocs = Map.of(
                "id-front", "http://storage.com/id-front.jpg",
                "selfie", "http://storage.com/selfie.jpg");

        // 2. Call OCR Activity
        log.info("Child: Calling OCR Activity");
        OCRActivity.OCRResult ocrResult = ocrActivity.extractText(mockDocs.get("id-front"), "ID_CARD");

        Map<String, Object> ocrData = new HashMap<>(ocrResult.getExtractedData());
        ocrData.put("ocrStatus", "SUCCESS");

        // 3. Call ID Verification Activity
        log.info("Child: Calling ID Verification Activity");
        IDVerificationActivity.IDVerificationResult verifyResult = idVerificationActivity.verifyID(
                (String) initialData.getOrDefault("idNumber", "123456789"),
                (String) initialData.getOrDefault("fullName", "Test User"),
                (String) initialData.getOrDefault("dob", "1990-01-01"),
                mockDocs.get("selfie"));

        Map<String, Object> result = new HashMap<>();
        result.put("documentsProcessed", true);
        result.put("ocrData", ocrData);
        result.put("verificationScore", verifyResult.getConfidenceScore());
        result.put("isVerified", verifyResult.isVerified());

        log.info("Child Workflow: DocumentProcessing completed. Verified: {}", verifyResult.isVerified());
        return result;
    }
}
