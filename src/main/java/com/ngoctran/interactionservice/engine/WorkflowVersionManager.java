package com.ngoctran.interactionservice.engine;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Workflow Version Manager - Mimics Temporal's Workflow.getVersion()
 * Allows code to evolve while maintaining backward compatibility with running
 * workflows.
 */
@Slf4j
public class WorkflowVersionManager {

    // Thread-local storage for version decisions made during this workflow
    // execution
    private static final ThreadLocal<Map<String, Integer>> versionDecisions = ThreadLocal.withInitial(HashMap::new);

    /**
     * Get the version for a specific change ID.
     * On first execution: Records the maxVersion and returns it.
     * On replay: Returns the previously recorded version from history.
     * 
     * @param changeId   Unique identifier for this code change point
     * @param minVersion Minimum supported version (for cleanup of old code)
     * @param maxVersion Current version of the code
     * @return The version to use for this execution
     */
    public static int getVersion(String changeId, int minVersion, int maxVersion) {
        WorkflowContext ctx = WorkflowContext.get();

        // Check if we already made a decision for this changeId in this execution
        Integer cachedVersion = versionDecisions.get().get(changeId);
        if (cachedVersion != null) {
            return cachedVersion;
        }

        int decidedVersion;

        if (ctx != null && ctx.isReplaying()) {
            // REPLAY MODE: Look for VERSION_MARKER event in history
            com.ngoctran.interactionservice.domain.WorkflowEvent versionEvent = ctx.nextEvent("VERSION_MARKER",
                    changeId);

            if (versionEvent != null && versionEvent.getCodeVersion() != null) {
                decidedVersion = versionEvent.getCodeVersion();
                log.info("[REPLAY] Using historical version {} for changeId: {}", decidedVersion, changeId);
            } else {
                // No version marker found in history, assume minVersion (oldest code path)
                decidedVersion = minVersion;
                log.warn("[REPLAY] No version marker found for changeId: {}, defaulting to minVersion: {}",
                        changeId, minVersion);
            }
        } else {
            // NORMAL EXECUTION: Use maxVersion (latest code)
            decidedVersion = maxVersion;
            log.info("[EXECUTION] Using current version {} for changeId: {}", decidedVersion, changeId);
        }

        // Cache the decision
        versionDecisions.get().put(changeId, decidedVersion);

        return decidedVersion;
    }

    /**
     * Clear version decisions (call at the end of workflow execution)
     */
    public static void clear() {
        versionDecisions.remove();
    }

    /**
     * Record a version marker event (called by WorkflowHistoryService)
     */
    public static void recordVersionMarker(UUID instanceId, String changeId, int version,
            com.ngoctran.interactionservice.service.WorkflowHistoryService historyService) {
        WorkflowContext ctx = WorkflowContext.get();

        // Only record during normal execution, not during replay
        if (ctx == null || !ctx.isReplaying()) {
            Map<String, Object> payload = Map.of(
                    "changeId", changeId,
                    "version", version,
                    "timestamp", java.time.Instant.now().toString());

            historyService.recordEvent(instanceId, "VERSION_MARKER", changeId, payload, "SYSTEM", version);
            log.debug("Recorded version marker: changeId={}, version={}", changeId, version);
        }
    }
}
