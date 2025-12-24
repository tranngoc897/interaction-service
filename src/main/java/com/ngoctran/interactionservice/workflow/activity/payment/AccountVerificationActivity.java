package com.ngoctran.interactionservice.workflow.activity.payment;

import io.temporal.activity.ActivityInterface;

/**
 * Account Verification Activity
 * Verifies account status and available balance
 */
@ActivityInterface
public interface AccountVerificationActivity {

    /**
     * Verify account status and balance
     * @param accountId Account to verify
     * @param requiredAmount Amount needed for the payment
     * @param currency Payment currency
     * @return Account verification result
     */
    AccountVerificationResult verifyAccount(String accountId, double requiredAmount, String currency);

    /**
     * Account verification result DTO
     */
    class AccountVerificationResult {
        private boolean accountExists;
        private boolean accountActive;
        private boolean sufficientFunds;
        private double availableBalance;
        private double currentBalance;
        private String accountStatus;
        private String accountType;
        private String errorMessage;

        public AccountVerificationResult() {}

        public AccountVerificationResult(boolean accountExists, boolean accountActive, boolean sufficientFunds,
                                       double availableBalance, double currentBalance, String accountStatus,
                                       String accountType, String errorMessage) {
            this.accountExists = accountExists;
            this.accountActive = accountActive;
            this.sufficientFunds = sufficientFunds;
            this.availableBalance = availableBalance;
            this.currentBalance = currentBalance;
            this.accountStatus = accountStatus;
            this.accountType = accountType;
            this.errorMessage = errorMessage;
        }

        // Getters and setters
        public boolean isAccountExists() { return accountExists; }
        public void setAccountExists(boolean accountExists) { this.accountExists = accountExists; }

        public boolean isAccountActive() { return accountActive; }
        public void setAccountActive(boolean accountActive) { this.accountActive = accountActive; }

        public boolean isSufficientFunds() { return sufficientFunds; }
        public void setSufficientFunds(boolean sufficientFunds) { this.sufficientFunds = sufficientFunds; }

        public double getAvailableBalance() { return availableBalance; }
        public void setAvailableBalance(double availableBalance) { this.availableBalance = availableBalance; }

        public double getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }

        public String getAccountStatus() { return accountStatus; }
        public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        /**
         * Check if account verification passed
         */
        public boolean isVerificationPassed() {
            return accountExists && accountActive && sufficientFunds;
        }
    }
}
