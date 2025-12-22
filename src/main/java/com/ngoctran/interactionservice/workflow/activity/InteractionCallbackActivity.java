package com.ngoctran.interactionservice.workflow.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.Map;

@ActivityInterface
public interface InteractionCallbackActivity {
    
    @ActivityMethod
    void updateInteractionStatus(
            String interactionId,
            String status,
            String reason,
            Map<String, Object> data
    );
    
    @ActivityMethod
    void updateCaseData(String caseId, Map<String, Object> data);
}
