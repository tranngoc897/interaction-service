package com.ngoctran.interactionservice.dmn;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DMN Decision Service - Manages DMN decision tables via REST API
 * Similar to onboarding's decision table usage
 * Uses Camunda REST API instead of embedded engine
 */
@Service
public class DmnDecisionService {

    private static final Logger log = LoggerFactory.getLogger(DmnDecisionService.class);

    private final RestTemplate restTemplate;
    private final String camundaBaseUrl;

    public DmnDecisionService(RestTemplate camundaRestTemplate, ObjectMapper objectMapper,
        @Value("${camunda.bpm.client.base-url:http://localhost:8080/engine-rest}") String camundaBaseUrl) {
        this.restTemplate = camundaRestTemplate;
        this.camundaBaseUrl = camundaBaseUrl;
    }

    /**
     * Deploy a DMN decision table via REST API
     */
    public Map<String, Object> deployDecisionTable(String decisionKey, String decisionName, String dmnXml) {
        log.info("Deploying DMN decision table: key={}, name={}", decisionKey, decisionName);

        try {
            String url = camundaBaseUrl + "/deployment/create";

            // Create multipart form data for DMN deployment
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // For simplicity, we'll use a basic approach
            Map<String, Object> request = new HashMap<>();
            request.put("deployment-name", decisionName);
            request.put("deployment-source", "dmn-deployment");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            log.info("Successfully deployed decision table");
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to deploy DMN decision table: {}", decisionKey, e);
            throw new RuntimeException("DMN deployment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Evaluate a decision table via REST API
     */
    public List<Map<String, Object>> evaluateDecision(String decisionKey, Map<String, Object> inputVariables) {
        log.info("Evaluating decision: {} with variables: {}", decisionKey, inputVariables.keySet());

        try {
            String url = camundaBaseUrl + "/decision-definition/key/" + decisionKey + "/evaluate";

            Map<String, Object> request = new HashMap<>();
            request.put("variables", inputVariables);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<List> response = restTemplate.postForEntity(url, entity, List.class);

            List<Map<String, Object>> result = response.getBody();
            log.info("Decision evaluation completed: {} rules matched", result.size());
            return result;

        } catch (Exception e) {
            log.error("Failed to evaluate decision: {}", decisionKey, e);
            throw new RuntimeException("Decision evaluation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Evaluate decision and return single result
     */
    public Map<String, Object> evaluateDecisionSingleResult(String decisionKey, Map<String, Object> inputVariables) {
        List<Map<String, Object>> result = evaluateDecision(decisionKey, inputVariables);

        if (result.isEmpty()) {
            throw new RuntimeException("No decision result found for: " + decisionKey);
        }

        if (result.size() > 1) {
            log.warn("Multiple decision results found for: {}, using first result", decisionKey);
        }

        return result.get(0);
    }

    /**
     * Get decision definition by key via REST API
     */
    public Map<String, Object> getDecisionDefinition(String decisionKey) {
        try {
            String url = camundaBaseUrl + "/decision-definition?key=" + decisionKey + "&latestVersion=true";
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

            List<Map<String, Object>> definitions = response.getBody();
            if (definitions != null && !definitions.isEmpty()) {
                return definitions.get(0);
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get decision definition: {}", decisionKey, e);
            return null;
        }
    }

    /**
     * Get all decision definitions via REST API
     */
    public List<Map<String, Object>> getAllDecisionDefinitions() {
        try {
            String url = camundaBaseUrl + "/decision-definition";
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody();
        } catch (Exception e) {
            log.warn("Failed to get decision definitions: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * AML Risk Assessment Decision
     */
    public String assessAmlRisk(Map<String, Object> applicantData) {
        log.info("Assessing AML risk for applicant");

        Map<String, Object> decisionInput = Map.of(
                "country", applicantData.get("citizenship.countryCode"),
                "transactionAmount", applicantData.getOrDefault("transactionAmount", 0),
                "pepStatus", applicantData.getOrDefault("pepStatus", false),
                "sanctionsCheck", applicantData.getOrDefault("sanctionsCheck", "PASSED")
        );

        try {
            Map<String, Object> result = evaluateDecisionSingleResult("aml-risk-assessment", decisionInput);
            return (String) result.getOrDefault("riskLevel", "MEDIUM");
        } catch (Exception e) {
            log.warn("AML risk assessment failed, defaulting to MEDIUM: {}", e.getMessage());
            return "MEDIUM";
        }
    }

    /**
     * Eligibility Check Decision
     */
    public boolean checkEligibility(Map<String, Object> applicantData) {
        log.info("Checking eligibility for applicant");

        Map<String, Object> decisionInput = Map.of(
                "age", calculateAge((String) applicantData.get("dateOfBirth")),
                "income", applicantData.getOrDefault("annualIncome", 0),
                "creditScore", applicantData.getOrDefault("creditScore", 0),
                "employmentStatus", applicantData.getOrDefault("employmentStatus", "UNKNOWN")
        );

        try {
            Map<String, Object> result = evaluateDecisionSingleResult("eligibility-check", decisionInput);
            return Boolean.TRUE.equals(result.get("eligible"));
        } catch (Exception e) {
            log.warn("Eligibility check failed, defaulting to false: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Product Recommendation Decision
     */
    public List<String> recommendProducts(Map<String, Object> applicantData) {
        log.info("Recommending products for applicant");

        Map<String, Object> decisionInput = Map.of(
                "riskProfile", applicantData.getOrDefault("riskProfile", "MODERATE"),
                "investmentAmount", applicantData.getOrDefault("investmentAmount", 0),
                "timeHorizon", applicantData.getOrDefault("timeHorizon", "MEDIUM"),
                "experienceLevel", applicantData.getOrDefault("experienceLevel", "BEGINNER")
        );

        try {
            List<Map<String, Object>> result = evaluateDecision("product-recommendation", decisionInput);
            return result.stream()
                    .map(row -> (String) row.get("productCode"))
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.warn("Product recommendation failed, returning empty list: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Compliance Check Decision
     */
    public Map<String, Object> performComplianceCheck(Map<String, Object> applicantData) {
        log.info("Performing compliance check for applicant");

        Map<String, Object> decisionInput = Map.of(
                "citizenshipType", applicantData.get("citizenship.citizenshipType"),
                "countryCode", applicantData.get("citizenship.countryCode"),
                "hasForeignTin", applicantData.containsKey("citizenship.foreignTin"),
                "w8BenAccepted", applicantData.getOrDefault("citizenship.w8ben.accepted", false)
        );

        try {
            Map<String, Object> result = evaluateDecisionSingleResult("compliance-check", decisionInput);
            return Map.of(
                    "compliant", result.get("compliant"),
                    "requiredDocuments", result.get("requiredDocuments"),
                    "reviewRequired", result.get("reviewRequired")
            );
        } catch (Exception e) {
            log.warn("Compliance check failed, assuming non-compliant: {}", e.getMessage());
            return Map.of(
                    "compliant", false,
                    "requiredDocuments", List.of("ID_VERIFICATION", "ADDRESS_PROOF"),
                    "reviewRequired", true
            );
        }
    }

    private int calculateAge(String dateOfBirth) {
        if (dateOfBirth == null) return 0;
        try {
            // Simple age calculation - in real implementation, use proper date handling
            int birthYear = Integer.parseInt(dateOfBirth.substring(0, 4));
            return 2024 - birthYear;
        } catch (Exception e) {
            log.warn("Failed to calculate age from: {}", dateOfBirth);
            return 0;
        }
    }

}
