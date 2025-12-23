package com.ngoctran.interactionservice.camel;

import com.ngoctran.interactionservice.workflow.TemporalWorkflowService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sample Camel Route showing how to start Temporal Workflow from Camel.
 * 
 * This demonstrates the "New API" approach where we use Camel to bridge
 * external events (REST, MQ, Files) with our Temporal-based Workflow Engine.
 */
@Component
@RequiredArgsConstructor
public class WorkflowStartRoute extends RouteBuilder {

    private final TemporalWorkflowService workflowService;

    @Override
    public void configure() throws Exception {

        // 1. Configure REST DSL
        restConfiguration()
                .component("servlet")
                .bindingMode(RestBindingMode.json);

        // 2. Define REST Endpoint to start KYC Workflow
        rest("/camel/workflow")
                .post("/start-kyc")
                .type(KycStartPayload.class)
                .to("direct:startKycWorkflow");

        // 3. Route Logic
        from("direct:startKycWorkflow")
                .routeId("start-kyc-workflow-route")
                .log("Camel received request to start KYC for Case: ${body.caseId}")

                // Transform and prepare data
                .process(exchange -> {
                    KycStartPayload payload = exchange.getIn().getBody(KycStartPayload.class);

                    // Call the Temporal Service
                    String processId = workflowService.startKYCOnboardingWorkflow(
                            payload.getCaseId(),
                            payload.getInteractionId(),
                            payload.getUserId(),
                            payload.getInitialData());

                    // Set result back to body
                    exchange.getIn().setBody(Map.of(
                            "status", "SUCCESS",
                            "processInstanceId", processId,
                            "message", "Workflow started via Apache Camel"));
                })
                .log("Workflow started successfully via Camel: ${body}");
    }

    /**
     * DTO for incoming Camel JSON payload
     */
    @lombok.Data
    public static class KycStartPayload {
        private String caseId;
        private String interactionId;
        private String userId;
        private Map<String, Object> initialData;
    }
}
