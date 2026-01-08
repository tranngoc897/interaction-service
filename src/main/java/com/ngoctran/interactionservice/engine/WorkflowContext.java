package com.ngoctran.interactionservice.engine;

import com.ngoctran.interactionservice.domain.WorkflowEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Context to track if we are currently replaying a workflow history.
 * Mimics Temporal's replay mechanism.
 */
public class WorkflowContext {
    private static final ThreadLocal<WorkflowContext> current = new ThreadLocal<>();

    @Getter
    @Setter
    private boolean replaying = false;

    @Getter
    private List<WorkflowEvent> history;

    private int historyCursor = 0;

    public static void set(WorkflowContext context) {
        current.set(context);
    }

    public static WorkflowContext get() {
        return current.get();
    }

    public static void clear() {
        current.remove();
    }

    public void setHistory(List<WorkflowEvent> history) {
        this.history = history;
        this.historyCursor = 0;
    }

    /**
     * Finds the next event in history that matches the criteria.
     * Used during replay to "simulate" a result instead of re-executing.
     */
    public WorkflowEvent nextEvent(String type, String name) {
        if (!replaying || history == null)
            return null;

        while (historyCursor < history.size()) {
            WorkflowEvent event = history.get(historyCursor++);
            if (event.getEventType().equals(type) && event.getEventName().equals(name)) {
                return event;
            }
        }
        return null; // Side effect not found in history, might be a bug in new code version
    }
}
