package com.ngoctran.interactionservice.workflow.activity.onboarding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CorebankAccountActivityImpl implements CorebankAccountActivity {

    @Override
    public AccountResult createVNDAccount(String customerId, String cifNumber, String coreCustomerId) {
        log.info("Creating VND account for customer: {}, CIF: {}", customerId, cifNumber);

        try {
            Thread.sleep(1500 + (int)(Math.random() * 2000)); // 1.5-3.5 seconds

            String accountNumber = "VN" + String.format("%014d", System.nanoTime() % 100000000000000L);
            String accountId = "ACC" + String.format("%012d", System.nanoTime() % 1000000000000L);

            boolean success = Math.random() > 0.03; // 97% success rate

            if (success) {
                log.info("VND account created: accountNumber={}, accountId={}", accountNumber, accountId);
                return new AccountResult(true, accountNumber, accountId, "VND", "SAVINGS", null, System.currentTimeMillis());
            } else {
                String errorMessage = "Corebank account creation failed";
                log.error("VND account creation failed: {}", errorMessage);
                return new AccountResult(false, null, null, "VND", "SAVINGS", errorMessage, System.currentTimeMillis());
            }

        } catch (Exception e) {
            log.error("VND account creation failed for customer: {}", customerId, e);
            return new AccountResult(false, null, null, "VND", "SAVINGS", "Account creation error: " + e.getMessage(), System.currentTimeMillis());
        }
    }
}
