package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.bpmn.FlowableEventBridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event-Driven OCR Processing Delegate
 */
@Component("eventDrivenOcrProcessingDelegate")
@RequiredArgsConstructor
@Slf4j
public class EventDrivenOcrProcessingDelegate implements JavaDelegate {

    private final FlowableEventBridge eventBridge;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String caseId = (String) execution.getVariable("caseId");

        log.info("Creating event-driven OCR processing job for process: {}, case: {}",
                processInstanceId, caseId);

        Map<String, Object> jobData = Map.of(
            "caseId", caseId,
            "processInstanceId", processInstanceId,
            "documentType", execution.getVariable("documentType"),
            "documentUrls", execution.getVariable("documentUrls"),
            "processingOptions", Map.of(
                "language", "auto",
                "confidenceThreshold", 0.8,
                "extractTables", true
            ),
            "timestamp", System.currentTimeMillis()
        );

        String jobId = eventBridge.createExternalJob("ocr-processing", processInstanceId, jobData);

        execution.setVariable("ocrJobId", jobId);
        execution.setVariable("ocrStatus", "PENDING");
        execution.setVariable("ocrStartedAt", System.currentTimeMillis());

        log.info("Created OCR processing job: {} for case: {}", jobId, caseId);
    }
}
