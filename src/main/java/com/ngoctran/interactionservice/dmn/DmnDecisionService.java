package com.ngoctran.interactionservice.dmn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.DecisionRequirementsDefinition;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DMN Decision Service - Manages DMN decision tables for business rules
 * Similar to ABB onboarding's decision table usage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DmnDecisionService {

    private final RepositoryService repositoryService;
    private final DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

    /**
     * Deploy a DMN decision table
     */
    public Deployment deployDecisionTable(String decisionKey, String decisionName, String dmnXml) {
        log.info("Deploying DMN decision table: key={}, name={}", decisionKey, decisionName);

        try {
            Deployment deployment = repositoryService.createDeployment()
                    .name(decisionName)
                    .addInputStream(decisionKey + ".dmn", new ByteArrayInputStream(dmnXml.getBytes()))
                    .deploy();

            log.info("Successfully deployed decision table: {}", deployment.getId());
            return deployment;
        } catch (Exception e) {
            log.error("Failed to deploy DMN decision table: {}", decisionKey, e);
            throw new RuntimeException("DMN deployment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Evaluate a decision table
     */
    public DmnDecisionTableResult evaluateDecision(String decisionKey, Map<String, Object> inputVariables) {
        log.info("Evaluating decision: {} with variables: {}", decisionKey, inputVariables.keySet());

        try {
            // Get the latest decision definition
            DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery()
                    .decisionDefinitionKey(decisionKey)
                    .latestVersion()
                    .singleResult();

            if (decisionDefinition == null) {
                throw new RuntimeException("Decision definition not found: " + decisionKey);
            }

            // Get DMN XML content
            String dmnXml = new String(repositoryService.getDecisionModel(decisionDefinition.getId()).toString());

            // Parse and evaluate
            List<DmnDecision> decisions = dmnEngine.parseDecisions(new ByteArrayInputStream(dmnXml.getBytes()));
            DmnDecision decision = decisions.stream()
                    .filter(d -> d.getKey().equals(decisionKey))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Decision not found in DMN: " + decisionKey));

            DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(decision, inputVariables);

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
        DmnDecisionTableResult result = evaluateDecision(decisionKey, inputVariables);

        if (result.isEmpty()) {
            throw new RuntimeException("No decision result found for: " + decisionKey);
        }

        if (result.size() > 1) {
            log.warn("Multiple decision results found for: {}, using first result", decisionKey);
        }

        return result.get(0);
    }

    /**
     * Get decision definition by key
     */
    public Optional<DecisionDefinition> getDecisionDefinition(String decisionKey) {
        return Optional.ofNullable(repositoryService.createDecisionDefinitionQuery()
                .decisionDefinitionKey(decisionKey)
                .latestVersion()
                .singleResult());
    }

    /**
     * Get all decision definitions
     */
    public List<DecisionDefinition> getAllDecisionDefinitions() {
        return repositoryService.createDecisionDefinitionQuery().list();
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
            DmnDecisionTableResult result = evaluateDecision("product-recommendation", decisionInput);
            return result.stream()
                    .map(row -> (String) row.get("productCode"))
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
