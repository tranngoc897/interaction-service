package com.ngoctran.interactionservice.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Rule Engine for evaluating transition conditions
 * Based on PDF specifications for JSON-based rule evaluation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEngine {

    private final ObjectMapper objectMapper;

    /**
     * Evaluate a condition against workflow context
     * @param condition JSON condition like "otp_status == SUCCESS"
     * @param context Workflow context data
     * @return true if condition passes
     */
    public boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null || condition.trim().isEmpty()) {
            return true; // No condition means always pass
        }

        try {
            // Simple condition evaluation (can be extended with more complex rules)
            return evaluateSimpleCondition(condition, context);
        } catch (Exception ex) {
            log.error("Error evaluating condition '{}': {}", condition, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Evaluate multiple conditions (AND logic)
     */
    public boolean evaluateConditions(String[] conditions, Map<String, Object> context) {
        if (conditions == null || conditions.length == 0) {
            return true;
        }

        for (String condition : conditions) {
            if (!evaluateCondition(condition, context)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateSimpleCondition(String condition, Map<String, Object> context) {
        // Parse simple conditions like "otp_status == SUCCESS"
        String[] parts = condition.trim().split("\\s+");

        if (parts.length != 3) {
            log.warn("Unsupported condition format: {}", condition);
            return false;
        }

        String field = parts[0];
        String operator = parts[1];
        String expectedValue = parts[2];

        Object actualValue = context.get(field);
        if (actualValue == null) {
            return false;
        }

        String actualValueStr = actualValue.toString();

        switch (operator) {
            case "==":
            case "=":
                return expectedValue.equals(actualValueStr);
            case "!=":
                return !expectedValue.equals(actualValueStr);
            case ">":
                return compareNumbers(actualValueStr, expectedValue) > 0;
            case "<":
                return compareNumbers(actualValueStr, expectedValue) < 0;
            case ">=":
                return compareNumbers(actualValueStr, expectedValue) >= 0;
            case "<=":
                return compareNumbers(actualValueStr, expectedValue) <= 0;
            default:
                log.warn("Unsupported operator: {}", operator);
                return false;
        }
    }

    private int compareNumbers(String value1, String value2) {
        try {
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            return Double.compare(num1, num2);
        } catch (NumberFormatException ex) {
            return value1.compareTo(value2);
        }
    }

    /**
     * Evaluate complex JSON conditions
     */
    public boolean evaluateJsonCondition(JsonNode condition, Map<String, Object> context) {
        // For more complex conditions, can implement JSON-based rules
        // For now, return true
        return true;
    }
}
