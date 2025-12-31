package com.ngoctran.interactionservice.controller;

import com.ngoctran.interactionservice.bpmn.BpmnProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * BPMN Process Controller - REST API for BPMN process operations
 * Provides endpoints for BPMN process deployment and execution
 */
@RestController
@RequestMapping("/api/bpmn")
public class BpmnProcessController {

    private static final Logger log = LoggerFactory.getLogger(BpmnProcessController.class);

    private final BpmnProcessService bpmnProcessService;

    public BpmnProcessController(BpmnProcessService bpmnProcessService) {
        this.bpmnProcessService = bpmnProcessService;
    }

    /**
     * Deploy a BPMN process
     */
    @PostMapping("/deploy")
    public ResponseEntity<com.ngoctran.interactionservice.controller.Deployment> deployProcess(
            @RequestParam String processKey,
            @RequestParam String processName,
            @RequestBody String bpmnXml) {

        log.info("Deploying BPMN process: key={}, name={}", processKey, processName);

        try {
            com.ngoctran.interactionservice.bpmn.Deployment deployment = bpmnProcessService.deployProcess(processKey, processName, bpmnXml);
            // Convert to controller DTO
            com.ngoctran.interactionservice.controller.Deployment result = new com.ngoctran.interactionservice.controller.Deployment();
            result.id = deployment.id;
            result.name = deployment.name;
            result.deploymentTime = deployment.deploymentTime;
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to deploy BPMN process", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Start a process instance
     */
    @PostMapping("/start")
    public ResponseEntity<com.ngoctran.interactionservice.controller.ProcessInstance> startProcess(
            @RequestParam String processDefinitionKey,
            @RequestParam(required = false) String businessKey,
            @RequestBody(required = false) Map<String, Object> variables) {

        log.info("Starting process instance: key={}, businessKey={}", processDefinitionKey, businessKey);

        try {
            com.ngoctran.interactionservice.bpmn.ProcessInstance instance = bpmnProcessService.startProcess(processDefinitionKey, businessKey, variables);
            // Convert to controller DTO
            com.ngoctran.interactionservice.controller.ProcessInstance result = new com.ngoctran.interactionservice.controller.ProcessInstance();
            result.id = instance.id;
            result.businessKey = instance.businessKey;
            result.processDefinitionId = instance.processDefinitionId;
            result.ended = instance.ended;
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to start process instance", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get process instance by business key
     */
    @GetMapping("/instance/{businessKey}")
    public ResponseEntity<com.ngoctran.interactionservice.controller.ProcessInstance> getProcessInstance(@PathVariable String businessKey) {
        Optional<com.ngoctran.interactionservice.bpmn.ProcessInstance> instanceOpt = bpmnProcessService.getProcessInstance(businessKey);
        if (instanceOpt.isPresent()) {
            com.ngoctran.interactionservice.bpmn.ProcessInstance instance = instanceOpt.get();
            // Convert to controller DTO
            com.ngoctran.interactionservice.controller.ProcessInstance result = new com.ngoctran.interactionservice.controller.ProcessInstance();
            result.id = instance.id;
            result.businessKey = instance.businessKey;
            result.processDefinitionId = instance.processDefinitionId;
            result.ended = instance.ended;
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get process instances by definition key
     */
    @GetMapping("/instances")
    public ResponseEntity<List<com.ngoctran.interactionservice.controller.ProcessInstance>> getProcessInstances(@RequestParam String processDefinitionKey) {
        List<com.ngoctran.interactionservice.bpmn.ProcessInstance> instances = bpmnProcessService.getProcessInstances(processDefinitionKey);
        // Convert to controller DTOs
        List<com.ngoctran.interactionservice.controller.ProcessInstance> result = instances.stream()
            .map(instance -> {
                com.ngoctran.interactionservice.controller.ProcessInstance dto = new com.ngoctran.interactionservice.controller.ProcessInstance();
                dto.id = instance.id;
                dto.businessKey = instance.businessKey;
                dto.processDefinitionId = instance.processDefinitionId;
                dto.ended = instance.ended;
                return dto;
            })
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Update process variables
     */
    @PutMapping("/variables/{processInstanceId}")
    public ResponseEntity<Void> updateVariables(
            @PathVariable String processInstanceId,
            @RequestBody Map<String, Object> variables) {

        try {
            bpmnProcessService.updateVariables(processInstanceId, variables);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to update process variables", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get process variables
     */
    @GetMapping("/variables/{processInstanceId}")
    public ResponseEntity<Map<String, Object>> getVariables(@PathVariable String processInstanceId) {
        try {
            Map<String, Object> variables = bpmnProcessService.getVariables(processInstanceId);
            return ResponseEntity.ok(variables);
        } catch (Exception e) {
            log.error("Failed to get process variables", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Signal a process instance
     */
    @PostMapping("/signal/{processInstanceId}")
    public ResponseEntity<Void> signalProcess(
            @PathVariable String processInstanceId,
            @RequestParam String signalName,
            @RequestBody(required = false) Map<String, Object> signalData) {

        try {
            bpmnProcessService.signalProcess(processInstanceId, signalName, signalData);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to signal process", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Correlate message to process instance
     */
    @PostMapping("/message")
    public ResponseEntity<Void> correlateMessage(
            @RequestParam String messageName,
            @RequestParam String businessKey,
            @RequestBody(required = false) Map<String, Object> messageData) {

        try {
            bpmnProcessService.correlateMessage(messageName, businessKey, messageData);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to correlate message", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete process instance
     */
    @DeleteMapping("/instance/{processInstanceId}")
    public ResponseEntity<Void> deleteProcessInstance(
            @PathVariable String processInstanceId,
            @RequestParam(defaultValue = "Deleted via API") String reason) {

        try {
            bpmnProcessService.deleteProcessInstance(processInstanceId, reason);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to delete process instance", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get process definition
     */
    @GetMapping("/definition/{processDefinitionKey}")
    public ResponseEntity<com.ngoctran.interactionservice.controller.ProcessDefinition> getProcessDefinition(@PathVariable String processDefinitionKey) {
        Optional<com.ngoctran.interactionservice.bpmn.ProcessDefinition> definitionOpt = bpmnProcessService.getProcessDefinition(processDefinitionKey);
        if (definitionOpt.isPresent()) {
            com.ngoctran.interactionservice.bpmn.ProcessDefinition definition = definitionOpt.get();
            // Convert to controller DTO
            com.ngoctran.interactionservice.controller.ProcessDefinition result = new com.ngoctran.interactionservice.controller.ProcessDefinition();
            result.id = definition.id;
            result.key = definition.key;
            result.name = definition.name;
            result.version = definition.version;
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Check if process instance is active
     */
    @GetMapping("/active/{businessKey}")
    public ResponseEntity<Boolean> isProcessActive(@PathVariable String businessKey) {
        boolean active = bpmnProcessService.isProcessActive(businessKey);
        return ResponseEntity.ok(active);
    }

    /**
     * Suspend process instance
     */
    @PostMapping("/suspend/{processInstanceId}")
    public ResponseEntity<Void> suspendProcess(@PathVariable String processInstanceId) {
        try {
            bpmnProcessService.suspendProcess(processInstanceId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to suspend process", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Activate suspended process instance
     */
    @PostMapping("/activate/{processInstanceId}")
    public ResponseEntity<Void> activateProcess(@PathVariable String processInstanceId) {
        try {
            bpmnProcessService.activateProcess(processInstanceId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to activate process", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
// Custom DTOs for REST API responses
class Deployment {
    public String id;
    public String name;
    public String deploymentTime;
}

class ProcessInstance {
    public String id;
    public String businessKey;
    public String processDefinitionId;
    public boolean ended;
}

class ProcessDefinition {
    public String id;
    public String key;
    public String name;
    public int version;
}
