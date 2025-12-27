package com.ngoctran.interactionservice.workflow.activity;

import io.temporal.activity.ActivityInterface;
import java.util.List;

/**
 * Retail Banking Account Creation Activity
 * Creates VND and USD accounts in Retail Banking System
 */
@ActivityInterface
public interface RetailAccountActivity {

    /**
     * Create VND and USD accounts in retail banking system
     */
    RetailAccountResult createRetailAccounts(String customerId, String cifNumber, String coreCustomerId);

    /**
     * Retail account creation result
     */
    class RetailAccountResult {
        private boolean success;
        private List<AccountInfo> accounts;
        private String errorMessage;
        private long createdAt;

        public RetailAccountResult() {}

        public RetailAccountResult(boolean success, List<AccountInfo> accounts,
                                 String errorMessage, long createdAt) {
            this.success = success;
            this.accounts = accounts;
            this.errorMessage = errorMessage;
            this.createdAt = createdAt;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<AccountInfo> getAccounts() { return accounts; }
        public void setAccounts(List<AccountInfo> accounts) { this.accounts = accounts; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Account information
     */
    class AccountInfo {
        private String accountNumber;
        private String accountId;
        private String currency;
        private String accountType;
        private String branchCode;

        public AccountInfo() {}

        public AccountInfo(String accountNumber, String accountId, String currency,
                         String accountType, String branchCode) {
            this.accountNumber = accountNumber;
            this.accountId = accountId;
            this.currency = currency;
            this.accountType = accountType;
            this.branchCode = branchCode;
        }

        // Getters and setters
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }

        public String getBranchCode() { return branchCode; }
        public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    }
}
