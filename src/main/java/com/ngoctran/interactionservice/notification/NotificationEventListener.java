package com.ngoctran.interactionservice.notification;

import com.ngoctran.interactionservice.events.MilestoneEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for MilestoneEvents to trigger automated notifications.
 * Uses @Async to ensure business logic is not blocked by notification sending.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    @Async
    public void handleMilestoneEvent(MilestoneEvent event) {
        log.info("NotificationEventListener: Capturing milestone {} for case {}",
                event.getMilestoneKey(), event.getCaseId());

        notificationService.processMilestoneNotification(
                event.getCaseId(),
                event.getMilestoneKey(),
                event.getEventType(),
                event.getEventData());
    }
}
