package com.ngoctran.interactionservice.workflow.activity.payment;

import io.temporal.activity.ActivityInterface;

/**
 * Payment Validation Activity
 * Validates payment details before processing
 */
@ActivityInterface
public interface PaymentValidationActivity {

    /**
     * Validate payment details
     * @param paymentId Unique payment identifier
     * @param accountId Account making the payment
     * @param amount Payment amount
     * @param currency Payment currency
     * @return Validation result
     */
    ValidationResult validatePayment(String paymentId, String accountId, double amount, String currency);

    /**
     * Validation result DTO
     */
    class ValidationResult {
        private boolean valid;
        private String errorMessage;
        private ValidationDetails details;

        public ValidationResult() {}

        public ValidationResult(boolean valid, String errorMessage, ValidationDetails details) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.details = details;
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public ValidationDetails getDetails() { return details; }
        public void setDetails(ValidationDetails details) { this.details = details; }
    }

    /**
     * Detailed validation information
     */
    class ValidationDetails {
        private boolean amountValid;
        private boolean currencyValid;
        private boolean accountValid;
        private String validatedAccountId;
        private String validatedCurrency;

        public ValidationDetails() {}

        public ValidationDetails(boolean amountValid, boolean currencyValid, boolean accountValid,
                               String validatedAccountId, String validatedCurrency) {
            this.amountValid = amountValid;
            this.currencyValid = currencyValid;
            this.accountValid = accountValid;
            this.validatedAccountId = validatedAccountId;
            this.validatedCurrency = validatedCurrency;
        }

        // Getters and setters
        public boolean isAmountValid() { return amountValid; }
        public void setAmountValid(boolean amountValid) { this.amountValid = amountValid; }

        public boolean isCurrencyValid() { return currencyValid; }
        public void setCurrencyValid(boolean currencyValid) { this.currencyValid = currencyValid; }

        public boolean isAccountValid() { return accountValid; }
        public void setAccountValid(boolean accountValid) { this.accountValid = accountValid; }

        public String getValidatedAccountId() { return validatedAccountId; }
        public void setValidatedAccountId(String validatedAccountId) { this.validatedAccountId = validatedAccountId; }

        public String getValidatedCurrency() { return validatedCurrency; }
        public void setValidatedCurrency(String validatedCurrency) { this.validatedCurrency = validatedCurrency; }
    }
}
