package com.ngoctran.interactionservice.workflow.activity.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account Verification Activity Implementation
 * Verifies account status and available balance
 */
@Component
@Slf4j
public class AccountVerificationActivityImpl implements AccountVerificationActivity {

    // Mock account database (in real implementation, this would be a database call)
    private static final Map<String, AccountData> ACCOUNT_DATABASE = new ConcurrentHashMap<>();

    static {
        // Initialize with some test accounts
        ACCOUNT_DATABASE.put("1234567890123456", new AccountData("ACTIVE", "SAVINGS", 10000000.0, 10000000.0));
        ACCOUNT_DATABASE.put("1234567890123457", new AccountData("ACTIVE", "CURRENT", 5000000.0, 5000000.0));
        ACCOUNT_DATABASE.put("1234567890123458", new AccountData("BLOCKED", "SAVINGS", 0.0, 0.0));
        ACCOUNT_DATABASE.put("1234567890123459", new AccountData("ACTIVE", "SAVINGS", 100000.0, 100000.0));
    }

    @Override
    public AccountVerificationResult verifyAccount(String accountId, double requiredAmount, String currency) {
        log.info("Verifying account: id={}, requiredAmount={}, currency={}",
                accountId, requiredAmount, currency);

        try {
            // Get account data (in real implementation, this would query the banking database)
            AccountData accountData = ACCOUNT_DATABASE.get(accountId);

            if (accountData == null) {
                log.warn("Account not found: {}", accountId);
                return new AccountVerificationResult(false, false, false, 0.0, 0.0,
                        "NOT_FOUND", null, "Account does not exist");
            }

            // Check account status
            boolean accountActive = "ACTIVE".equals(accountData.status);
            boolean accountExists = true;

            // Check sufficient funds
            boolean sufficientFunds = accountData.availableBalance >= requiredAmount;

            // Build result
            String errorMessage = buildErrorMessage(accountExists, accountActive, sufficientFunds);

            AccountVerificationResult result = new AccountVerificationResult(
                    accountExists,
                    accountActive,
                    sufficientFunds,
                    accountData.availableBalance,
                    accountData.currentBalance,
                    accountData.status,
                    accountData.type,
                    errorMessage
            );

            log.info("Account verification completed: exists={}, active={}, sufficientFunds={}, balance={}",
                    accountExists, accountActive, sufficientFunds, accountData.availableBalance);

            return result;

        } catch (Exception e) {
            log.error("Account verification failed for account: {}", accountId, e);
            return new AccountVerificationResult(false, false, false, 0.0, 0.0,
                    "ERROR", null, "Verification service error: " + e.getMessage());
        }
    }

    /**
     * Build error message based on verification results
     */
    private String buildErrorMessage(boolean accountExists, boolean accountActive, boolean sufficientFunds) {
        if (!accountExists) {
            return "Account does not exist";
        }

        if (!accountActive) {
            return "Account is not active";
        }

        if (!sufficientFunds) {
            return "Insufficient funds";
        }

        return null; // No errors
    }

    /**
     * Mock account data structure
     */
    private static class AccountData {
        final String status;
        final String type;
        final double currentBalance;
        final double availableBalance;

        AccountData(String status, String type, double currentBalance, double availableBalance) {
            this.status = status;
            this.type = type;
            this.currentBalance = currentBalance;
            this.availableBalance = availableBalance;
        }
    }
}
