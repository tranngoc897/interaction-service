package com.ngoctran.interactionservice;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
public class KafkaPublisher {

    private final KafkaTemplate<String, Object> template;
    public KafkaPublisher(KafkaTemplate<String,Object> template){ this.template = template; }

    public void publishCaseEvent(UUID caseId, String eventType, Map<String,Object> payload){
        var envelope = Map.of(
            "caseId", caseId.toString(),
            "type", eventType,
            "payload", payload,
            "timestamp", System.currentTimeMillis()
        );
        template.send("case.events", caseId.toString(), envelope);
    }
}