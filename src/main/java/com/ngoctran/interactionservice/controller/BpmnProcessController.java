package com.ngoctran.interactionservice.controller;

import com.ngoctran.interactionservice.bpmn.BpmnProcessService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
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
    public ResponseEntity<Deployment> deployProcess(
            @RequestParam String processKey,
            @RequestParam String processName,
            @RequestBody String bpmnXml) {

        log.info("Deploying BPMN process: key={}, name={}", processKey, processName);

        try {
            Deployment deployment = bpmnProcessService.deployProcess(processKey, processName, bpmnXml);
            return ResponseEntity.ok(deployment);
        } catch (Exception e) {
            log.error("Failed to deploy BPMN process", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Start a process instance
     */
    @PostMapping("/start")
    public ResponseEntity<ProcessInstance> startProcess(
            @RequestParam String processDefinitionKey,
            @RequestParam(required = false) String businessKey,
            @RequestBody(required = false) Map<String, Object> variables) {

        log.info("Starting process instance: key={}, businessKey={}", processDefinitionKey, businessKey);

        try {
            ProcessInstance instance = bpmnProcessService.startProcess(processDefinitionKey, businessKey, variables);
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            log.error("Failed to start process instance", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get process instance by business key
     */
    @GetMapping("/instance/{businessKey}")
    public ResponseEntity<ProcessInstance> getProcessInstance(@PathVariable String businessKey) {
        Optional<ProcessInstance> instance = bpmnProcessService.getProcessInstance(businessKey);
        return instance.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get process instances by definition key
     */
    @GetMapping("/instances")
    public ResponseEntity<List<ProcessInstance>> getProcessInstances(@RequestParam String processDefinitionKey) {
        List<ProcessInstance> instances = bpmnProcessService.getProcessInstances(processDefinitionKey);
        return ResponseEntity.ok(instances);
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
    public ResponseEntity<ProcessDefinition> getProcessDefinition(@PathVariable String processDefinitionKey) {
        Optional<ProcessDefinition> definition = bpmnProcessService.getProcessDefinition(processDefinitionKey);
        return definition.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
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
