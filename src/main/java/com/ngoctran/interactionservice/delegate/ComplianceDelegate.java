package com.ngoctran.interactionservice.delegate;

import com.ngoctran.interactionservice.compliance.ComplianceService;
import com.ngoctran.interactionservice.dmn.DmnDecisionService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JavaDelegate for BPMN compliance checks
 * This is an alias for ComplianceCheckDelegate to match BPMN configuration
 * Called from BPMN processes to perform compliance validation
 */
@Component("complianceDelegate")
public class ComplianceDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ComplianceDelegate.class);

    private final ComplianceService complianceService;
    private final DmnDecisionService dmnDecisionService;

    public ComplianceDelegate(ComplianceService complianceService, DmnDecisionService dmnDecisionService) {
        this.complianceService = complianceService;
        this.dmnDecisionService = dmnDecisionService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing compliance check for process: {}", execution.getProcessInstanceId());

        try {
            // Get case data from process variables
            String caseId = (String) execution.getVariable("caseId");
            String applicantId = (String) execution.getVariable("applicantId");
            @SuppressWarnings("unchecked")
            Map<String, Object> applicantData = (Map<String, Object>) execution.getVariable("applicantData");

            if (applicantData == null) {
                log.warn("No applicant data found for compliance check");
                execution.setVariable("compliancePassed", false);
                execution.setVariable("complianceStatus", "FAILED");
                return;
            }

            // Perform AML screening
            var amlResult = complianceService.performAmlScreening(caseId, applicantId, applicantData);
            boolean amlPassed = "PASSED".equals(amlResult.getStatus());

            // Perform KYC verification
            var kycResult = complianceService.performKycVerification(caseId, applicantId, applicantData);
            boolean kycPassed = "PASSED".equals(kycResult.getStatus());

            // Perform sanctions screening
            var sanctionsResult = complianceService.performSanctionsScreening(caseId, applicantId, applicantData);
            boolean sanctionsPassed = "PASSED".equals(sanctionsResult.getStatus());

            // Use DMN for risk assessment
            String riskLevel = dmnDecisionService.assessAmlRisk(applicantData);

            // Overall compliance result
            boolean overallPassed = amlPassed && kycPassed && sanctionsPassed;
            String overallStatus = overallPassed ? "PASSED" : "REVIEW_NEEDED";

            // Set process variables for BPMN flow
            execution.setVariable("compliancePassed", overallPassed);
            execution.setVariable("complianceStatus", overallStatus);
            execution.setVariable("riskLevel", riskLevel);
            execution.setVariable("amlStatus", amlResult.getStatus());
            execution.setVariable("kycStatus", kycResult.getStatus());
            execution.setVariable("sanctionsStatus", sanctionsResult.getStatus());

            log.info("Compliance check completed: passed={}, riskLevel={}, aml={}, kyc={}, sanctions={}",
                    overallPassed, riskLevel, amlResult.getStatus(), kycResult.getStatus(),
                    sanctionsResult.getStatus());

        } catch (Exception e) {
            log.error("Compliance check failed: {}", e.getMessage(), e);
            execution.setVariable("compliancePassed", false);
            execution.setVariable("complianceStatus", "ERROR");
            execution.setVariable("complianceError", e.getMessage());
        }
    }
}
