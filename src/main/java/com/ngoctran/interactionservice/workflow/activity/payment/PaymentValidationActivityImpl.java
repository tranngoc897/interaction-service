package com.ngoctran.interactionservice.workflow.activity.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Payment Validation Activity Implementation
 * Validates payment details before processing
 */
@Component
@Slf4j
public class PaymentValidationActivityImpl implements PaymentValidationActivity {

    // Supported currencies
    private static final List<String> SUPPORTED_CURRENCIES = Arrays.asList("VND", "USD", "EUR", "SGD");

    // Minimum and maximum payment amounts
    private static final double MIN_PAYMENT_AMOUNT = 1000.0; // VND
    private static final double MAX_PAYMENT_AMOUNT = 500000000.0; // 500M VND

    @Override
    public ValidationResult validatePayment(String paymentId, String accountId, double amount, String currency) {
        log.info("Validating payment: id={}, account={}, amount={}, currency={}",
                paymentId, accountId, amount, currency);

        try {
            ValidationDetails details = new ValidationDetails();

            // Validate amount
            boolean amountValid = validateAmount(amount);
            details.setAmountValid(amountValid);

            // Validate currency
            boolean currencyValid = validateCurrency(currency);
            details.setCurrencyValid(currencyValid);
            if (currencyValid) {
                details.setValidatedCurrency(currency.toUpperCase());
            }

            // Validate account ID
            boolean accountValid = validateAccountId(accountId);
            details.setAccountValid(accountValid);
            if (accountValid) {
                details.setValidatedAccountId(accountId.trim());
            }

            // Overall validation result
            boolean isValid = amountValid && currencyValid && accountValid;
            String errorMessage = null;

            if (!isValid) {
                StringBuilder errors = new StringBuilder();
                if (!amountValid) errors.append("Invalid amount. ");
                if (!currencyValid) errors.append("Unsupported currency. ");
                if (!accountValid) errors.append("Invalid account ID. ");
                errorMessage = errors.toString().trim();
            }

            ValidationResult result = new ValidationResult(isValid, errorMessage, details);
            log.info("Payment validation completed: valid={}, errors={}", isValid, errorMessage);

            return result;

        } catch (Exception e) {
            log.error("Payment validation failed for payment: {}", paymentId, e);
            return new ValidationResult(false, "Validation service error: " + e.getMessage(), null);
        }
    }

    /**
     * Validate payment amount
     */
    private boolean validateAmount(double amount) {
        if (amount <= 0) {
            log.warn("Payment amount must be positive: {}", amount);
            return false;
        }

        if (amount < MIN_PAYMENT_AMOUNT) {
            log.warn("Payment amount below minimum: {} < {}", amount, MIN_PAYMENT_AMOUNT);
            return false;
        }

        if (amount > MAX_PAYMENT_AMOUNT) {
            log.warn("Payment amount above maximum: {} > {}", amount, MAX_PAYMENT_AMOUNT);
            return false;
        }

        // Check for reasonable decimal places (max 2)
        if ((amount * 100) != Math.floor(amount * 100)) {
            log.warn("Payment amount has too many decimal places: {}", amount);
            return false;
        }

        return true;
    }

    /**
     * Validate currency code
     */
    private boolean validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            log.warn("Currency is required");
            return false;
        }

        String upperCurrency = currency.toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(upperCurrency)) {
            log.warn("Unsupported currency: {} (supported: {})", upperCurrency, SUPPORTED_CURRENCIES);
            return false;
        }

        return true;
    }

    /**
     * Validate account ID format
     */
    private boolean validateAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            log.warn("Account ID is required");
            return false;
        }

        String trimmedId = accountId.trim();

        // Basic account ID validation (adjust based on your bank's format)
        // Example: Should be numeric, 8-20 digits
        if (!trimmedId.matches("\\d{8,20}")) {
            log.warn("Invalid account ID format: {} (should be 8-20 digits)", trimmedId);
            return false;
        }

        // Additional validation could include:
        // - Checksum validation
        // - Account existence in database
        // - Account status verification

        return true;
    }
}
