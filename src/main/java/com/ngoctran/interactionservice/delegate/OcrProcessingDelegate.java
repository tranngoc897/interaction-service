package com.ngoctran.interactionservice.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JavaDelegate for OCR (Optical Character Recognition) processing
 * Extracts text and data from uploaded documents (ID cards, passports, etc.)
 */
@Component("ocrProcessingDelegate")
public class OcrProcessingDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(OcrProcessingDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing OCR processing for process: {}", execution.getProcessInstanceId());

        try {
            // Get document information from process variables
            String caseId = (String) execution.getVariable("caseId");
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadedDocuments = (Map<String, Object>) execution.getVariable("uploadedDocuments");

            if (uploadedDocuments == null || uploadedDocuments.isEmpty()) {
                log.warn("No documents found for OCR processing");
                execution.setVariable("ocrCompleted", false);
                execution.setVariable("ocrStatus", "NO_DOCUMENTS");
                return;
            }

            log.info("Processing {} documents for case: {}", uploadedDocuments.size(), caseId);

            // Process each document type
            Map<String, Object> ocrResults = new HashMap<>();
            boolean allSuccessful = true;

            // Process ID document
            if (uploadedDocuments.containsKey("idDocument")) {
                String idDocumentPath = (String) uploadedDocuments.get("idDocument");
                Map<String, Object> idOcrResult = processIdDocument(idDocumentPath);
                ocrResults.put("idDocument", idOcrResult);

                if (!"SUCCESS".equals(idOcrResult.get("status"))) {
                    allSuccessful = false;
                }
            }

            // Process proof of address
            if (uploadedDocuments.containsKey("proofOfAddress")) {
                String addressDocPath = (String) uploadedDocuments.get("proofOfAddress");
                Map<String, Object> addressOcrResult = processAddressDocument(addressDocPath);
                ocrResults.put("proofOfAddress", addressOcrResult);

                if (!"SUCCESS".equals(addressOcrResult.get("status"))) {
                    allSuccessful = false;
                }
            }

            // Process income proof
            if (uploadedDocuments.containsKey("incomeProof")) {
                String incomeDocPath = (String) uploadedDocuments.get("incomeProof");
                Map<String, Object> incomeOcrResult = processIncomeDocument(incomeDocPath);
                ocrResults.put("incomeProof", incomeOcrResult);

                if (!"SUCCESS".equals(incomeOcrResult.get("status"))) {
                    allSuccessful = false;
                }
            }

            // Set process variables for BPMN flow
            execution.setVariable("ocrCompleted", allSuccessful);
            execution.setVariable("ocrStatus", allSuccessful ? "SUCCESS" : "PARTIAL_SUCCESS");
            execution.setVariable("ocrResults", ocrResults);
            execution.setVariable("extractedData", extractDataFromOcrResults(ocrResults));

            log.info("OCR processing completed: success={}, documentsProcessed={}",
                    allSuccessful, ocrResults.size());

        } catch (Exception e) {
            log.error("OCR processing failed: {}", e.getMessage(), e);
            execution.setVariable("ocrCompleted", false);
            execution.setVariable("ocrStatus", "ERROR");
            execution.setVariable("ocrError", e.getMessage());
            throw e;
        }
    }

    /**
     * Process ID document (passport, national ID, driver's license)
     */
    private Map<String, Object> processIdDocument(String documentPath) {
        log.info("Processing ID document: {}", documentPath);

        try {
            // Simulate OCR API call
            Thread.sleep(1000);

            // Simulate OCR extraction
            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("documentType", "NATIONAL_ID");
            result.put("extractedFields", Map.of(
                    "idNumber", "ID" + System.currentTimeMillis(),
                    "fullName", "JOHN DOE",
                    "dateOfBirth", "1990-01-15",
                    "nationality", "US",
                    "issueDate", "2020-01-01",
                    "expiryDate", "2030-01-01",
                    "address", "123 Main St, City, State"));
            result.put("confidence", 0.95);
            result.put("documentQuality", "HIGH");

            log.info("ID document processed successfully with confidence: {}", result.get("confidence"));
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted during OCR processing", e);
            return Map.of("status", "ERROR", "error", "Processing interrupted");
        } catch (Exception e) {
            log.error("Error processing ID document: {}", e.getMessage(), e);
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    /**
     * Process proof of address document
     */
    private Map<String, Object> processAddressDocument(String documentPath) {
        log.info("Processing address document: {}", documentPath);

        try {
            // Simulate OCR API call
            Thread.sleep(800);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("documentType", "UTILITY_BILL");
            result.put("extractedFields", Map.of(
                    "fullName", "John Doe",
                    "address", "123 Main St, City, State, 12345",
                    "documentDate", "2024-12-01",
                    "utilityType", "ELECTRICITY",
                    "accountNumber", "ACC123456"));
            result.put("confidence", 0.92);

            log.info("Address document processed successfully");
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("status", "ERROR", "error", "Processing interrupted");
        } catch (Exception e) {
            log.error("Error processing address document: {}", e.getMessage(), e);
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    /**
     * Process income proof document
     */
    private Map<String, Object> processIncomeDocument(String documentPath) {
        log.info("Processing income document: {}", documentPath);

        try {
            // Simulate OCR API call
            Thread.sleep(900);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("documentType", "PAYSLIP");
            result.put("extractedFields", Map.of(
                    "employeeName", "John Doe",
                    "employerName", "ABC Corporation",
                    "monthlyIncome", 5000.00,
                    "payPeriod", "2024-12",
                    "employeeId", "EMP12345"));
            result.put("confidence", 0.90);

            log.info("Income document processed successfully");
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("status", "ERROR", "error", "Processing interrupted");
        } catch (Exception e) {
            log.error("Error processing income document: {}", e.getMessage(), e);
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    /**
     * Extract and consolidate data from OCR results
     */
    private Map<String, Object> extractDataFromOcrResults(Map<String, Object> ocrResults) {
        Map<String, Object> consolidatedData = new HashMap<>();

        // Extract from ID document
        if (ocrResults.containsKey("idDocument")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> idResult = (Map<String, Object>) ocrResults.get("idDocument");
            if ("SUCCESS".equals(idResult.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fields = (Map<String, Object>) idResult.get("extractedFields");
                consolidatedData.putAll(fields);
            }
        }

        // Extract from address document
        if (ocrResults.containsKey("proofOfAddress")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> addressResult = (Map<String, Object>) ocrResults.get("proofOfAddress");
            if ("SUCCESS".equals(addressResult.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fields = (Map<String, Object>) addressResult.get("extractedFields");
                consolidatedData.put("verifiedAddress", fields.get("address"));
            }
        }

        // Extract from income document
        if (ocrResults.containsKey("incomeProof")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> incomeResult = (Map<String, Object>) ocrResults.get("incomeProof");
            if ("SUCCESS".equals(incomeResult.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fields = (Map<String, Object>) incomeResult.get("extractedFields");
                consolidatedData.put("monthlyIncome", fields.get("monthlyIncome"));
                consolidatedData.put("employer", fields.get("employerName"));
            }
        }

        return consolidatedData;
    }
}
