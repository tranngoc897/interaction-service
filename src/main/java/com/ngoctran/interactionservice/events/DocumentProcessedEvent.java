package com.ngoctran.interactionservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Document Processed Event - Published after OCR and Document Verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessedEvent {
    private String caseId;
    private String documentType; // ID_CARD, PASSPORT, PROOF_OF_ADDRESS, INCOME_PROOF
    private String status; // SUCCESS, FAILED
    private double confidenceScore;
    private boolean verificationResult;
    private Map<String, Object> metadata;
    private long timestamp;
}
