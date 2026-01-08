package com.ngoctran.interactionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.domain.WorkflowEvent;
import com.ngoctran.interactionservice.repo.WorkflowEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowHistoryService {

    private final WorkflowEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Record an event in the history
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(UUID instanceId, String type, String name, Object payload, String actor) {
        recordEvent(instanceId, type, name, payload, actor, 1); // Default to version 1
    }

    /**
     * Record an event in the history with specific code version
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(UUID instanceId, String type, String name, Object payload, String actor, int codeVersion) {
        try {
            int nextSeq = eventRepository.findMaxSequenceNumber(instanceId) + 1;

            WorkflowEvent event = WorkflowEvent.builder()
                    .instanceId(instanceId)
                    .eventType(type)
                    .eventName(name)
                    .payload(objectMapper.writeValueAsString(payload))
                    .sequenceNumber(nextSeq)
                    .codeVersion(codeVersion)
                    .createdBy(actor)
                    .build();

            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to record workflow event for instance {}", instanceId, e);
        }
    }

}
