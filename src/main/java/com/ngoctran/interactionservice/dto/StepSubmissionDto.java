package com.ngoctran.interactionservice.dto;

import java.util.Map;

public class StepSubmissionDto {
    private String stepName;
    private Map<String, Object> stepData;
    private Map<String, Object> clientContext;

    public StepSubmissionDto() {
    }

    public StepSubmissionDto(String stepName, Map<String, Object> stepData, Map<String, Object> clientContext) {
        this.stepName = stepName;
        this.stepData = stepData;
        this.clientContext = clientContext;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Map<String, Object> getStepData() {
        return stepData;
    }

    public void setStepData(Map<String, Object> stepData) {
        this.stepData = stepData;
    }

    public Map<String, Object> getClientContext() {
        return clientContext;
    }

    public void setClientContext(Map<String, Object> clientContext) {
        this.clientContext = clientContext;
    }
}
