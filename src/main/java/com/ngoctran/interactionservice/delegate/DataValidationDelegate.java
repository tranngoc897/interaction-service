package com.ngoctran.interactionservice.delegate;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * JavaDelegate for data validation and sanitization
 * Validates and sanitizes applicant data before processing
 */
@Component("dataValidationDelegate")
public class DataValidationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(DataValidationDelegate.class);

    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$" // E.164 format
    );

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z\\s'-]{2,100}$");

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing data validation for process: {}", execution.getProcessInstanceId());

        try {
            // Get applicant data
            @SuppressWarnings("unchecked")
            Map<String, Object> applicantData = (Map<String, Object>) execution.getVariable("applicantData");

            if (applicantData == null || applicantData.isEmpty()) {
                throw new BpmnError("VALIDATION_ERROR", "Applicant data is missing or empty");
            }

            List<String> validationErrors = new ArrayList<>();

            // Validate required fields
            validateRequiredFields(applicantData, validationErrors);

            // Validate field formats
            validateFieldFormats(applicantData, validationErrors);

            // Validate business rules
            validateBusinessRules(applicantData, validationErrors);

            // If there are validation errors, throw BPMN error
            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join(", ", validationErrors);
                log.error("Data validation failed: {}", errorMessage);

                execution.setVariable("validationErrors", validationErrors);
                execution.setVariable("validationPassed", false);

                throw new BpmnError("VALIDATION_ERROR", errorMessage);
            }

            // Sanitize data
            Map<String, Object> sanitizedData = sanitizeData(applicantData);
            execution.setVariable("applicantData", sanitizedData);

            // Set validation success variables
            execution.setVariable("validationPassed", true);
            execution.setVariable("validationErrors", List.of());

            log.info("Data validation completed successfully for process: {}",
                    execution.getProcessInstanceId());

        } catch (BpmnError e) {
            // Re-throw BPMN errors
            throw e;
        } catch (Exception e) {
            log.error("Data validation failed with exception: {}", e.getMessage(), e);
            throw new BpmnError("VALIDATION_ERROR",
                    "Validation failed: " + e.getMessage());
        }
    }

    /**
     * Validate required fields
     */
    private void validateRequiredFields(Map<String, Object> data, List<String> errors) {
        String[] requiredFields = {
                "fullName", "email", "phoneNumber", "dateOfBirth",
                "nationality", "idNumber"
        };

        for (String field : requiredFields) {
            Object value = data.get(field);
            if (value == null || value.toString().trim().isEmpty()) {
                errors.add("Required field missing: " + field);
            }
        }
    }

    /**
     * Validate field formats
     */
    private void validateFieldFormats(Map<String, Object> data, List<String> errors) {
        // Validate email
        String email = (String) data.get("email");
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Invalid email format: " + email);
        }

        // Validate phone number
        String phoneNumber = (String) data.get("phoneNumber");
        if (phoneNumber != null && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            errors.add("Invalid phone number format: " + phoneNumber);
        }

        // Validate full name
        String fullName = (String) data.get("fullName");
        if (fullName != null && !NAME_PATTERN.matcher(fullName).matches()) {
            errors.add("Invalid name format: " + fullName);
        }

        // Validate date of birth
        String dateOfBirth = (String) data.get("dateOfBirth");
        if (dateOfBirth != null && !isValidDate(dateOfBirth)) {
            errors.add("Invalid date of birth format: " + dateOfBirth);
        }
    }

    /**
     * Validate business rules
     */
    private void validateBusinessRules(Map<String, Object> data, List<String> errors) {
        // Validate age (must be 18+)
        String dateOfBirth = (String) data.get("dateOfBirth");
        if (dateOfBirth != null && !isAdult(dateOfBirth)) {
            errors.add("Applicant must be 18 years or older");
        }

        // Validate ID number length
        String idNumber = (String) data.get("idNumber");
        if (idNumber != null && (idNumber.length() < 5 || idNumber.length() > 20)) {
            errors.add("ID number must be between 5 and 20 characters");
        }

        // Validate nationality (should not be empty or invalid)
        String nationality = (String) data.get("nationality");
        if (nationality != null && nationality.length() < 2) {
            errors.add("Invalid nationality");
        }
    }

    /**
     * Sanitize data to prevent injection attacks
     */
    private Map<String, Object> sanitizeData(Map<String, Object> data) {
        // Create a new map with sanitized values
        Map<String, Object> sanitized = new java.util.HashMap<>(data);

        // Sanitize string fields
        for (Map.Entry<String, Object> entry : sanitized.entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();

                // Trim whitespace
                value = value.trim();

                // Remove potentially dangerous characters
                value = value.replaceAll("[<>\"']", "");

                // Limit length
                if (value.length() > 500) {
                    value = value.substring(0, 500);
                }

                sanitized.put(entry.getKey(), value);
            }
        }

        return sanitized;
    }

    /**
     * Validate date format (YYYY-MM-DD)
     */
    private boolean isValidDate(String date) {
        try {
            Pattern datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
            if (!datePattern.matcher(date).matches()) {
                return false;
            }

            String[] parts = date.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            if (year < 1900 || year > 2100)
                return false;
            if (month < 1 || month > 12)
                return false;
            if (day < 1 || day > 31)
                return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if person is 18 years or older
     */
    private boolean isAdult(String dateOfBirth) {
        try {
            String[] parts = dateOfBirth.split("-");
            int birthYear = Integer.parseInt(parts[0]);
            int currentYear = java.time.Year.now().getValue();

            int age = currentYear - birthYear;

            // Simple age check (doesn't account for exact birth date)
            return age >= 18;
        } catch (Exception e) {
            return false;
        }
    }
}
