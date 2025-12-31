package com.ngoctran.interactionservice.workflow.onboarding;

/**
 * Onboarding Workflow Interface
 * This is a placeholder interface for ABB onboarding integration
 */
public interface OnboardingWorkflow {

    /**
     * Start onboarding workflow
     */
    void startOnboarding(String caseId);

    /**
     * Complete onboarding workflow
     */
    void completeOnboarding(String caseId);

    /**
     * Handle document upload
     */
    void handleDocumentUpload(String caseId, String documentType);

    /**
     * Handle personal information submission
     */
    void handlePersonalInfo(String caseId);

    /**
     * Handle AML check
     */
    void handleAmlCheck(String caseId);

    /**
     * Handle manual review
     */
    void handleManualReview(String caseId, boolean approved);
}
