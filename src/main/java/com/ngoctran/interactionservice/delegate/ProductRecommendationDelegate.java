package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.dmn.DmnDecisionService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * JavaDelegate for BPMN product recommendations
 * Called from BPMN processes to recommend products based on applicant profile
 */
@Component("productRecommendationDelegate")
public class ProductRecommendationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ProductRecommendationDelegate.class);

    private final DmnDecisionService dmnDecisionService;

    public ProductRecommendationDelegate(DmnDecisionService dmnDecisionService) {
        this.dmnDecisionService = dmnDecisionService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing product recommendation for process: {}", execution.getProcessInstanceId());

        try {
            // Get applicant data from process variables
            @SuppressWarnings("unchecked")
            Map<String, Object> applicantData = (Map<String, Object>) execution.getVariable("applicantData");

            if (applicantData == null) {
                log.warn("No applicant data found for product recommendation");
                execution.setVariable("recommendedProducts", List.of());
                return;
            }

            // Use DMN for product recommendations
            List<String> recommendedProducts = dmnDecisionService.recommendProducts(applicantData);

            // Check eligibility using DMN
            boolean eligible = dmnDecisionService.checkEligibility(applicantData);

            // Set process variables for BPMN flow
            execution.setVariable("recommendedProducts", recommendedProducts);
            execution.setVariable("eligible", eligible);
            execution.setVariable("productCount", recommendedProducts.size());

            log.info("Product recommendation completed: eligible={}, products={}",
                    eligible, recommendedProducts);

        } catch (Exception e) {
            log.error("Product recommendation failed: {}", e.getMessage(), e);
            execution.setVariable("recommendedProducts", List.of());
            execution.setVariable("eligible", false);
            execution.setVariable("recommendationError", e.getMessage());
        }
    }
}
