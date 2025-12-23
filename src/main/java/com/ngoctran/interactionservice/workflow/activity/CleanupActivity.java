package com.ngoctran.interactionservice.workflow.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CleanupActivity {

    @ActivityMethod
    int cleanupstaleInteractions(int hoursOld);
}
