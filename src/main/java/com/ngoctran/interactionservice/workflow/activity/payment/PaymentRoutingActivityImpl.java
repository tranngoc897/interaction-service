package com.ngoctran.interactionservice.workflow.activity.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentRoutingActivityImpl implements PaymentRoutingActivity {

    @Override
    public RoutingDecision routePayment(String paymentId, double amount, String currency, String accountId) {
        log.info("Routing payment: id={}, amount={}, currency={}, account={}",
                paymentId, amount, currency, accountId);

        // Simple routing logic - in real implementation, this would be more sophisticated
        String processorName;
        String processorType;
        String routingReason;
        double estimatedFee = 0.0;
        long estimatedTime = 2000; // 2 seconds

        if (amount > 50000000) { // High value
            processorName = "BANK_SECURE_PROCESSOR";
            processorType = "HIGH_VALUE";
            routingReason = "High-value transaction routed to secure processor";
            estimatedFee = 5000; // Higher fee for secure processing
            estimatedTime = 5000; // Longer processing time
        } else if (currency.equals("USD")) {
            processorName = "INTERNATIONAL_PROCESSOR";
            processorType = "INTERNATIONAL";
            routingReason = "USD transaction routed to international processor";
            estimatedFee = 2000;
            estimatedTime = 3000;
        } else {
            processorName = "STANDARD_DOMESTIC_PROCESSOR";
            processorType = "DOMESTIC";
            routingReason = "Standard domestic transaction";
            estimatedFee = 1000;
            estimatedTime = 2000;
        }

        String fallbackProcessor = "EMERGENCY_PROCESSOR";

        RoutingDecision decision = new RoutingDecision(processorName, processorType,
                routingReason, estimatedFee, estimatedTime, fallbackProcessor);

        log.info("Payment routing decision: processor={}, type={}, fee={}, time={}ms",
                processorName, processorType, estimatedFee, estimatedTime);

        return decision;
    }
}
