package com.ngoctran.interactionservice.bpmn;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BPMN Process Service - Manages BPMN process deployment and execution
 * Similar to ABB onboarding's BPMN orchestration
 */
@Service
public class BpmnProcessService {

    private static final Logger log = LoggerFactory.getLogger(BpmnProcessService.class);

    private final ProcessEngine processEngine;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;

    public BpmnProcessService(ProcessEngine processEngine, RepositoryService repositoryService, RuntimeService runtimeService) {
        this.processEngine = processEngine;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
    }

    /**
     * Deploy a BPMN process from XML string
     */
    public Deployment deployProcess(String processKey, String processName, String bpmnXml) {
        log.info("Deploying BPMN process: key={}, name={}", processKey, processName);

        try {
            Deployment deployment = repositoryService.createDeployment()
                    .name(processName)
                    .addInputStream(processKey + ".bpmn", new ByteArrayInputStream(bpmnXml.getBytes()))
                    .deploy();

            log.info("Successfully deployed process: {}", deployment.getId());
            return deployment;
        } catch (Exception e) {
            log.error("Failed to deploy BPMN process: {}", processKey, e);
            throw new RuntimeException("BPMN deployment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Start a process instance
     */
    public ProcessInstance startProcess(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        log.info("Starting process instance: key={}, businessKey={}", processDefinitionKey, businessKey);

        try {
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                    processDefinitionKey, businessKey, variables);

            log.info("Started process instance: {}", instance.getId());
            return instance;
        } catch (Exception e) {
            log.error("Failed to start process instance: {}", processDefinitionKey, e);
            throw new RuntimeException("Process start failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get process instance by business key
     */
    public Optional<ProcessInstance> getProcessInstance(String businessKey) {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey);

        return Optional.ofNullable(query.singleResult());
    }

    /**
     * Get process instances by process definition key
     */
    public List<ProcessInstance> getProcessInstances(String processDefinitionKey) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .list();
    }

    /**
     * Update process variables
     */
    public void updateVariables(String processInstanceId, Map<String, Object> variables) {
        log.info("Updating variables for process instance: {}", processInstanceId);

        runtimeService.setVariables(processInstanceId, variables);
    }

    /**
     * Get process variables
     */
    public Map<String, Object> getVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    /**
     * Signal a process instance (for event-based processes)
     */
    public void signalProcess(String processInstanceId, String signalName, Map<String, Object> signalData) {
        log.info("Signaling process instance: {} with signal: {}", processInstanceId, signalName);

        if (signalData != null && !signalData.isEmpty()) {
            runtimeService.setVariables(processInstanceId, signalData);
        }
        runtimeService.signal(processInstanceId);
    }

    /**
     * Correlate message to process instance
     */
    public void correlateMessage(String messageName, String businessKey, Map<String, Object> messageData) {
        log.info("Correlating message: {} to business key: {}", messageName, businessKey);

        if (messageData != null && !messageData.isEmpty()) {
            runtimeService.correlateMessage(messageName, businessKey, messageData);
        } else {
            runtimeService.correlateMessage(messageName, businessKey);
        }
    }

    /**
     * Delete process instance
     */
    public void deleteProcessInstance(String processInstanceId, String reason) {
        log.info("Deleting process instance: {} with reason: {}", processInstanceId, reason);

        runtimeService.deleteProcessInstance(processInstanceId, reason);
    }

    /**
     * Get process definition by key
     */
    public Optional<ProcessDefinition> getProcessDefinition(String processDefinitionKey) {
        return Optional.ofNullable(repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .latestVersion()
                .singleResult());
    }

    /**
     * Check if process instance is active
     */
    public boolean isProcessActive(String businessKey) {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .active();

        return query.count() > 0;
    }

    /**
     * Suspend process instance
     */
    public void suspendProcess(String processInstanceId) {
        log.info("Suspending process instance: {}", processInstanceId);
        runtimeService.suspendProcessInstanceById(processInstanceId);
    }

    /**
     * Activate suspended process instance
     */
    public void activateProcess(String processInstanceId) {
        log.info("Activating process instance: {}", processInstanceId);
        runtimeService.activateProcessInstanceById(processInstanceId);
    }

    /**
     * Get process instance history
     */
    public List<org.camunda.bpm.engine.history.HistoricProcessInstance> getProcessHistory(String businessKey) {
        return processEngine.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .list();
    }
}
