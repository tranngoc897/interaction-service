package com.ngoctran.interactionservice.bpmn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.events.WorkflowEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BPMN Process Service - Manages BPMN process deployment and execution via REST
 * API
 * Connects to external Camunda server via HTTP REST calls
 * Similar to ABB onboarding's BPMN orchestration
 */
@Service
public class BpmnProcessService {

    private static final Logger log = LoggerFactory.getLogger(BpmnProcessService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WorkflowEventPublisher eventPublisher;
    private final String camundaBaseUrl;

    public BpmnProcessService(RestTemplate camundaRestTemplate, ObjectMapper objectMapper,
            WorkflowEventPublisher eventPublisher,
            @Value("${camunda.bpm.client.base-url:http://localhost:8080/engine-rest}") String camundaBaseUrl) {
        this.restTemplate = camundaRestTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.camundaBaseUrl = camundaBaseUrl;
    }

    /**
     * Deploy a BPMN process from XML string via REST API
     */
    public Deployment deployProcess(String processKey, String processName, String bpmnXml) {
        log.info("Deploying BPMN process: key={}, name={}", processKey, processName);

        try {
            String url = camundaBaseUrl + "/deployment/create";

            // Create multipart form data
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // For simplicity, we'll use a basic approach
            // In production, you'd use MultipartFile or similar
            Map<String, Object> request = new HashMap<>();
            request.put("deployment-name", processName);
            request.put("deployment-source", "process-application");
            // Note: File upload would need proper multipart handling

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Deployment> response = restTemplate.postForEntity(url, entity, Deployment.class);
            Deployment body = response.getBody();

            if (body != null) {
                log.info("Successfully deployed process: {}", body.id);

                // Publish deployment event
                eventPublisher.publishWorkflowStateEvent(body.id,
                        processKey, "NONE", "DEPLOYED",
                        Map.of("name", processName));
            }

            return body;
        } catch (Exception e) {
            log.error("Failed to deploy BPMN process: {}", processKey, e);
            throw new RuntimeException("BPMN deployment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Start a process instance via REST API
     */
    public ProcessInstance startProcess(String processDefinitionKey, String businessKey,
            Map<String, Object> variables) {
        log.info("Starting process instance: key={}, businessKey={}", processDefinitionKey, businessKey);

        try {
            String url = camundaBaseUrl + "/process-definition/key/" + processDefinitionKey + "/start";

            Map<String, Object> request = new HashMap<>();
            if (businessKey != null) {
                request.put("businessKey", businessKey);
            }
            if (variables != null && !variables.isEmpty()) {
                request.put("variables", variables);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<ProcessInstance> response = restTemplate.postForEntity(url, entity, ProcessInstance.class);

            log.info("Started process instance: {}", response.getBody().id);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to start process instance: {}", processDefinitionKey, e);
            throw new RuntimeException("Process start failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get process instance by business key via REST API
     */
    public Optional<ProcessInstance> getProcessInstance(String businessKey) {
        try {
            String url = camundaBaseUrl + "/process-instance?businessKey=" + businessKey;
            ResponseEntity<ProcessInstance[]> response = restTemplate.getForEntity(url, ProcessInstance[].class);

            ProcessInstance[] instances = response.getBody();
            if (instances != null && instances.length > 0) {
                return Optional.of(instances[0]);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to get process instance for business key: {}", businessKey, e);
            return Optional.empty();
        }
    }

    /**
     * Get process instances by process definition key via REST API
     */
    public List<ProcessInstance> getProcessInstances(String processDefinitionKey) {
        try {
            String url = camundaBaseUrl + "/process-instance?processDefinitionKey=" + processDefinitionKey;
            ResponseEntity<ProcessInstance[]> response = restTemplate.getForEntity(url, ProcessInstance[].class);

            return List.of(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to get process instances for definition: {}", processDefinitionKey, e);
            return List.of();
        }
    }

    /**
     * Update process variables via REST API
     */
    public void updateVariables(String processInstanceId, Map<String, Object> variables) {
        log.info("Updating variables for process instance: {}", processInstanceId);

        try {
            String url = camundaBaseUrl + "/process-instance/" + processInstanceId + "/variables";

            Map<String, Object> request = new HashMap<>();
            variables.forEach((key, value) -> {
                Map<String, Object> varData = new HashMap<>();
                varData.put("value", value);
                varData.put("type", getVariableType(value));
                request.put(key, varData);
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.error("Failed to update variables for process instance: {}", processInstanceId, e);
            throw new RuntimeException("Variable update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get process variables via REST API
     */
    public Map<String, Object> getVariables(String processInstanceId) {
        try {
            String url = camundaBaseUrl + "/process-instance/" + processInstanceId + "/variables";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            Map<String, Object> result = new HashMap<>();
            Map<String, Map<String, Object>> variables = response.getBody();
            if (variables != null) {
                variables.forEach((key, varData) -> {
                    result.put(key, varData.get("value"));
                });
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to get variables for process instance: {}", processInstanceId, e);
            return new HashMap<>();
        }
    }

    /**
     * Signal a process instance via REST API
     */
    public void signalProcess(String processInstanceId, String signalName, Map<String, Object> signalData) {
        log.info("Signaling process instance: {} with signal: {}", processInstanceId, signalName);

        try {
            String url = camundaBaseUrl + "/signal";

            Map<String, Object> request = new HashMap<>();
            request.put("name", signalName);
            if (signalData != null && !signalData.isEmpty()) {
                request.put("variables", signalData);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, String.class);

            // Publish signal event
            eventPublisher.publishWorkflowStateEvent(processInstanceId, "UNKNOWN",
                    "RUNNING", "SIGNALED",
                    Map.of("signalName", signalName, "data", signalData != null ? signalData : Map.of()));
        } catch (Exception e) {
            log.error("Failed to signal process: {}", processInstanceId, e);
            throw new RuntimeException("Signal failed: " + e.getMessage(), e);
        }
    }

    /**
     * Correlate message to process instance via REST API
     */
    public void correlateMessage(String messageName, String businessKey, Map<String, Object> messageData) {
        log.info("Correlating message: {} to business key: {}", messageName, businessKey);

        try {
            String url = camundaBaseUrl + "/message";

            Map<String, Object> request = new HashMap<>();
            request.put("messageName", messageName);
            request.put("businessKey", businessKey);
            if (messageData != null && !messageData.isEmpty()) {
                request.put("processVariables", messageData);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, String.class);

            // Publish message event
            eventPublisher.publishWorkflowStateEvent(businessKey, "UNKNOWN",
                    "RUNNING", "MESSAGE_CORRELATED",
                    Map.of("messageName", messageName, "data", messageData != null ? messageData : Map.of()));
        } catch (Exception e) {
            log.error("Failed to correlate message: {}", messageName, e);
            throw new RuntimeException("Message correlation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delete process instance via REST API
     */
    public void deleteProcessInstance(String processInstanceId, String reason) {
        log.info("Deleting process instance: {} with reason: {}", processInstanceId, reason);

        try {
            String url = camundaBaseUrl + "/process-instance/" + processInstanceId;
            restTemplate.delete(url);
        } catch (Exception e) {
            log.error("Failed to delete process instance: {}", processInstanceId, e);
            throw new RuntimeException("Process deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get process definition by key via REST API
     */
    public Optional<ProcessDefinition> getProcessDefinition(String processDefinitionKey) {
        try {
            String url = camundaBaseUrl + "/process-definition?key=" + processDefinitionKey + "&latestVersion=true";
            ResponseEntity<ProcessDefinition[]> response = restTemplate.getForEntity(url, ProcessDefinition[].class);

            ProcessDefinition[] definitions = response.getBody();
            if (definitions != null && definitions.length > 0) {
                return Optional.of(definitions[0]);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to get process definition: {}", processDefinitionKey, e);
            return Optional.empty();
        }
    }

    /**
     * Check if process instance is active via REST API
     */
    public boolean isProcessActive(String businessKey) {
        try {
            String url = camundaBaseUrl + "/process-instance?businessKey=" + businessKey + "&active=true";
            ResponseEntity<ProcessInstance[]> response = restTemplate.getForEntity(url, ProcessInstance[].class);

            ProcessInstance[] instances = response.getBody();
            return instances != null && instances.length > 0;
        } catch (Exception e) {
            log.warn("Failed to check process status for business key: {}", businessKey, e);
            return false;
        }
    }

    /**
     * Suspend process instance via REST API
     */
    public void suspendProcess(String processInstanceId) {
        log.info("Suspending process instance: {}", processInstanceId);

        try {
            String url = camundaBaseUrl + "/process-instance/" + processInstanceId + "/suspended";

            Map<String, Object> request = new HashMap<>();
            request.put("suspended", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.put(url, entity);

            // Publish suspend event
            eventPublisher.publishWorkflowStateEvent(processInstanceId, "UNKNOWN",
                    "RUNNING", "SUSPENDED", Map.of());
        } catch (Exception e) {
            log.error("Failed to suspend process instance: {}", processInstanceId, e);
            throw new RuntimeException("Process suspend failed: " + e.getMessage(), e);
        }
    }

    /**
     * Activate suspended process instance via REST API
     */
    public void activateProcess(String processInstanceId) {
        log.info("Activating process instance: {}", processInstanceId);

        try {
            String url = camundaBaseUrl + "/process-instance/" + processInstanceId + "/suspended";

            Map<String, Object> request = new HashMap<>();
            request.put("suspended", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.put(url, entity);

            // Publish activate event
            eventPublisher.publishWorkflowStateEvent(processInstanceId, "UNKNOWN",
                    "SUSPENDED", "ACTIVE", Map.of());
        } catch (Exception e) {
            log.error("Failed to activate process instance: {}", processInstanceId, e);
            throw new RuntimeException("Process activate failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get process instance history via REST API
     */
    public List<Map<String, Object>> getProcessHistory(String businessKey) {
        try {
            String url = camundaBaseUrl + "/history/process-instance?businessKey=" + businessKey;
            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

            return List.of(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to get process history for business key: {}", businessKey, e);
            return List.of();
        }
    }

    /**
     * Generate a migration plan between two process definitions
     */
    public Map<String, Object> generateMigrationPlan(String sourceDefinitionId, String targetDefinitionId) {
        log.info("Generating migration plan: source={}, target={}", sourceDefinitionId, targetDefinitionId);
        try {
            String url = camundaBaseUrl + "/migration/generate";
            Map<String, Object> request = new HashMap<>();
            request.put("sourceProcessDefinitionId", sourceDefinitionId);
            request.put("targetProcessDefinitionId", targetDefinitionId);
            request.put("updateEventTriggers", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to generate migration plan", e);
            throw new RuntimeException("Migration plan generation failed: " + e.getMessage());
        }
    }

    /**
     * Execute a migration plan for specific process instances
     */
    public void executeMigrationPlan(Map<String, Object> migrationPlan, List<String> processInstanceIds) {
        log.info("Executing migration plan for {} instances", processInstanceIds.size());
        try {
            String url = camundaBaseUrl + "/migration/execute";
            Map<String, Object> request = new HashMap<>();
            request.put("migrationPlan", migrationPlan);
            request.put("processInstanceIds", processInstanceIds);
            request.put("skipCustomListeners", true);
            request.put("skipIoMappings", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, String.class);

            eventPublisher.publishWorkflowStateEvent("SYSTEM", "BPMN_MIGRATION",
                    "RUNNING", "MIGRATED",
                    Map.of("instanceCount", processInstanceIds.size(), "target",
                            migrationPlan.get("targetProcessDefinitionId")));
        } catch (Exception e) {
            log.error("Failed to execute migration plan", e);
            throw new RuntimeException("Migration execution failed: " + e.getMessage());
        }
    }

    /**
     * Helper method to determine variable type for REST API
     */
    private String getVariableType(Object value) {
        if (value == null)
            return "Null";
        if (value instanceof String)
            return "String";
        if (value instanceof Integer)
            return "Integer";
        if (value instanceof Long)
            return "Long";
        if (value instanceof Double)
            return "Double";
        if (value instanceof Boolean)
            return "Boolean";
        return "Object";
    }
}
