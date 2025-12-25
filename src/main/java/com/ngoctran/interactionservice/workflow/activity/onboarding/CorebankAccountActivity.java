package com.ngoctran.interactionservice.workflow.activity.onboarding;

import io.temporal.activity.ActivityInterface;
import java.util.List;

/**
 * Corebank Account Creation Activity
 * Creates VND account in Core Banking System
 */
@ActivityInterface
public interface CorebankAccountActivity {

    /**
     * Create VND account in core banking system
     */
    AccountResult createVNDAccount(String customerId, String cifNumber, String coreCustomerId);

    /**
     * Account creation result
     */
    class AccountResult {
        private boolean success;
        private String accountNumber;
        private String accountId;
        private String currency;
        private String accountType;
        private String errorMessage;
        private long createdAt;

        public AccountResult() {}

        public AccountResult(boolean success, String accountNumber, String accountId,
                           String currency, String accountType, String errorMessage, long createdAt) {
            this.success = success;
            this.accountNumber = accountNumber;
            this.accountId = accountId;
            this.currency = currency;
            this.accountType = accountType;
            this.errorMessage = errorMessage;
            this.createdAt = createdAt;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }
}
