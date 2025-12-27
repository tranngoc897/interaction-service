package com.ngoctran.interactionservice.workflow.activity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class RetailAccountActivityImpl implements RetailAccountActivity {

    @Override
    public RetailAccountResult createRetailAccounts(String customerId, String cifNumber, String coreCustomerId) {
        log.info("Creating VND and USD retail accounts for customer: {}, CIF: {}", customerId, cifNumber);

        try {
            Thread.sleep(2000 + (int)(Math.random() * 3000)); // 2-5 seconds

            // Create VND account
            RetailAccountActivity.AccountInfo vndAccount = new RetailAccountActivity.AccountInfo(
                "RVN" + String.format("%014d", System.nanoTime() % 100000000000000L),
                "RACC" + String.format("%012d", System.nanoTime() % 1000000000000L),
                "VND",
                "CURRENT",
                "001"
            );

            // Create USD account
            RetailAccountActivity.AccountInfo usdAccount = new RetailAccountActivity.AccountInfo(
                "RUS" + String.format("%014d", (System.nanoTime() + 1) % 100000000000000L),
                "RACC" + String.format("%012d", (System.nanoTime() + 1) % 1000000000000L),
                "USD",
                "CURRENT",
                "001"
            );

            List<RetailAccountActivity.AccountInfo> accounts = Arrays.asList(vndAccount, usdAccount);
            boolean success = Math.random() > 0.04; // 96% success rate

            if (success) {
                log.info("Retail accounts created successfully: VND={}, USD={}",
                    vndAccount.getAccountNumber(), usdAccount.getAccountNumber());
                return new RetailAccountResult(true, accounts, null, System.currentTimeMillis());
            } else {
                String errorMessage = "Retail banking system temporarily unavailable";
                log.error("Retail accounts creation failed: {}", errorMessage);
                return new RetailAccountResult(false, null, errorMessage, System.currentTimeMillis());
            }

        } catch (Exception e) {
            log.error("Retail accounts creation failed for customer: {}", customerId, e);
            return new RetailAccountResult(false, null, "Retail accounts creation error: " + e.getMessage(), System.currentTimeMillis());
        }
    }
}
