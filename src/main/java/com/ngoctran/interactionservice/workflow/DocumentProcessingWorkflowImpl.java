package com.ngoctran.interactionservice.workflow;

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

        // Sub-process logic
        Map<String, String> mockDocs = Map.of("front", "url1", "back", "url2");

        log.info("Child: Performing OCR");
        Map<String, Object> ocrResults = new HashMap<>(initialData);
        ocrResults.put("ocrStatus", "SUCCESS");

        log.info("Child: Verifying ID");
        // In real world, we'd call activities here

        Map<String, Object> result = new HashMap<>();
        result.put("documentsProcessed", true);
        result.put("ocrData", ocrResults);
        result.put("verificationScore", 0.95);

        log.info("Child Workflow: DocumentProcessing completed");
        return result;
    }
}
