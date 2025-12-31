package com.ngoctran.interactionservice.dto;

import java.util.Map;

public class NextStepResponse {
    private String nextStep;
    private Map<String, Object> uiModel;
    private String status;

    public NextStepResponse() {
    }

    public NextStepResponse(String nextStep, Map<String, Object> uiModel, String status) {
        this.nextStep = nextStep;
        this.uiModel = uiModel;
        this.status = status;
    }

    public String getNextStep() {
        return nextStep;
    }

    public void setNextStep(String nextStep) {
        this.nextStep = nextStep;
    }

    public Map<String, Object> getUiModel() {
        return uiModel;
    }

    public void setUiModel(Map<String, Object> uiModel) {
        this.uiModel = uiModel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
