package com.ngoctran.interactionservice.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoctran.interactionservice.mapping.enums.EngineType;
import com.ngoctran.interactionservice.mapping.enums.ProcessStatus;
import com.ngoctran.interactionservice.mapping.exception.ProcessMappingNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Process Mappings
 * 
 * Handles the lifecycle of process mappings between business processes and workflow engines
 */
@Service
@RequiredArgsConstructor
public class ProcessMappingService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProcessMappingService.class);
    
    private final ProcessMappingRepository processMappingRepo;
    private final ObjectMapper objectMapper;
    
    /**
     * Create a new process mapping
     */
    @Transactional
    public ProcessMappingEntity createProcessMapping(
            EngineType engineType,
            String processInstanceId,
            String processDefinitionKey,
            String caseId,
            String userId) {
        
        return createProcessMapping(
                engineType,
                processInstanceId,
                processDefinitionKey,
                caseId,
                userId,
                null,
                null
        );
    }
    
    /**
     * Create a new process mapping with business key and metadata
     */
    @Transactional
    public ProcessMappingEntity createProcessMapping(
            EngineType engineType,
            String processInstanceId,
            String processDefinitionKey,
            String caseId,
            String userId,
            String businessKey,
            Map<String, Object> metadata) {
        
        log.info("Creating process mapping: engine={}, processInstanceId={}, caseId={}", 
                engineType, processInstanceId, caseId);
        
        ProcessMappingEntity mapping = new ProcessMappingEntity();
        mapping.setId(UUID.randomUUID().toString());
        mapping.setEngineType(engineType);
        mapping.setProcessInstanceId(processInstanceId);
        mapping.setProcessDefinitionKey(processDefinitionKey);
        mapping.setBusinessKey(businessKey != null ? businessKey : caseId);
        mapping.setCaseId(caseId);
        mapping.setUserId(userId);
        mapping.setStatus(ProcessStatus.RUNNING);
        mapping.setStartedAt(LocalDateTime.now());
        
        if (metadata != null && !metadata.isEmpty()) {
            try {
                String metadataJson = objectMapper.writeValueAsString(metadata);
                mapping.setMetadata(metadataJson);
            } catch (Exception e) {
                log.warn("Failed to serialize metadata", e);
            }
        }
        
        ProcessMappingEntity saved = processMappingRepo.save(mapping);
        
        log.info("Process mapping created: id={}", saved.getId());
        return saved;
    }
    
    /**
     * Update process status to COMPLETED
     */
    @Transactional
    public void markProcessCompleted(String processInstanceId) {
        log.info("Marking process as completed: {}", processInstanceId);
        
        ProcessMappingEntity mapping = findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new ProcessMappingNotFoundException(processInstanceId));
        
        mapping.markCompleted();
        processMappingRepo.save(mapping);
        
        log.info("Process marked as completed: {}", processInstanceId);
    }
    
    /**
     * Update process status to FAILED
     */
    @Transactional
    public void markProcessFailed(String processInstanceId, String errorMessage) {
        log.info("Marking process as failed: {}, error: {}", processInstanceId, errorMessage);
        
        ProcessMappingEntity mapping = findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new ProcessMappingNotFoundException(processInstanceId));
        
        mapping.markFailed(errorMessage);
        processMappingRepo.save(mapping);
        
        log.info("Process marked as failed: {}", processInstanceId);
    }
    
    /**
     * Update process status to CANCELLED
     */
    @Transactional
    public void markProcessCancelled(String processInstanceId) {
        log.info("Marking process as cancelled: {}", processInstanceId);
        
        ProcessMappingEntity mapping = findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new ProcessMappingNotFoundException(processInstanceId));
        
        mapping.markCancelled();
        processMappingRepo.save(mapping);
        
        log.info("Process marked as cancelled: {}", processInstanceId);
    }
    
    /**
     * Update process metadata
     */
    @Transactional
    public void updateProcessMetadata(String processInstanceId, Map<String, Object> metadata) {
        log.info("Updating process metadata: {}", processInstanceId);
        
        ProcessMappingEntity mapping = findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new ProcessMappingNotFoundException(processInstanceId));
        
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            mapping.setMetadata(metadataJson);
            processMappingRepo.save(mapping);
            
            log.info("Process metadata updated: {}", processInstanceId);
        } catch (Exception e) {
            log.error("Failed to update process metadata", e);
            throw new RuntimeException("Failed to update metadata", e);
        }
    }
    
    /**
     * Find process mapping by process instance ID
     */
    public Optional<ProcessMappingEntity> findByProcessInstanceId(String processInstanceId) {
        return processMappingRepo.findByProcessInstanceId(processInstanceId);
    }
    
    /**
     * Get all process mappings for a case
     */
    public List<ProcessMappingEntity> getProcessMappingsByCaseId(String caseId) {
        return processMappingRepo.findByCaseId(caseId);
    }
    
    /**
     * Get running processes for a case
     */
    public List<ProcessMappingEntity> getRunningProcessesByCaseId(String caseId) {
        return processMappingRepo.findRunningProcessesByCaseId(caseId);
    }
    
    /**
     * Get completed processes for a case
     */
    public List<ProcessMappingEntity> getCompletedProcessesByCaseId(String caseId) {
        return processMappingRepo.findCompletedProcessesByCaseId(caseId);
    }
    
    /**
     * Check if there's a running process for a case
     */
    public boolean hasRunningProcess(String caseId) {
        long count = processMappingRepo.countByCaseIdAndStatus(caseId, ProcessStatus.RUNNING);
        return count > 0;
    }
    
    /**
     * Check if a specific workflow type is running for a case
     */
    public boolean hasRunningProcess(String caseId, String processDefinitionKey) {
        return processMappingRepo.existsByCaseIdAndProcessDefinitionKey(caseId, processDefinitionKey);
    }
    
    /**
     * Get latest process for a case by definition key
     */
    public Optional<ProcessMappingEntity> getLatestProcess(String caseId, String processDefinitionKey) {
        return processMappingRepo.findFirstByCaseIdAndProcessDefinitionKeyOrderByStartedAtDesc(
                caseId, 
                processDefinitionKey
        );
    }
    
    /**
     * Get all processes for a user
     */
    public List<ProcessMappingEntity> getProcessMappingsByUserId(String userId) {
        return processMappingRepo.findByUserId(userId);
    }
    
    /**
     * Get processes by status
     */
    public List<ProcessMappingEntity> getProcessMappingsByStatus(ProcessStatus status) {
        return processMappingRepo.findByStatus(status);
    }
    
    /**
     * Get failed processes since a specific time
     */
    public List<ProcessMappingEntity> getFailedProcessesSince(LocalDateTime since) {
        return processMappingRepo.findFailedProcessesSince(since);
    }
    
    /**
     * Get long-running processes (running longer than specified duration)
     */
    public List<ProcessMappingEntity> getLongRunningProcesses(Duration duration) {
        LocalDateTime threshold = LocalDateTime.now().minus(duration);
        return processMappingRepo.findLongRunningProcesses(threshold);
    }
    
    /**
     * Get process statistics for a case
     */
    public ProcessStatistics getProcessStatistics(String caseId) {
        List<ProcessMappingEntity> allProcesses = processMappingRepo.findByCaseId(caseId);
        
        long total = allProcesses.size();
        long running = allProcesses.stream().filter(ProcessMappingEntity::isRunning).count();
        long completed = allProcesses.stream().filter(ProcessMappingEntity::isCompleted).count();
        long failed = allProcesses.stream().filter(ProcessMappingEntity::isFailed).count();
        long cancelled = allProcesses.stream().filter(ProcessMappingEntity::isCancelled).count();
        
        return new ProcessStatistics(total, running, completed, failed, cancelled);
    }
    
    /**
     * Get process summary for a case
     */
    public ProcessSummary getProcessSummary(String caseId) {
        List<ProcessMappingEntity> processes = processMappingRepo.findByCaseId(caseId);
        
        ProcessStatistics stats = getProcessStatistics(caseId);
        
        List<ProcessInfo> processInfos = processes.stream()
                .map(p -> new ProcessInfo(
                        p.getId(),
                        p.getProcessDefinitionKey(),
                        p.getStatus(),
                        p.getStartedAt(),
                        p.getCompletedAt()
                ))
                .collect(Collectors.toList());
        
        return new ProcessSummary(caseId, stats, processInfos);
    }
    
    /**
     * Delete process mapping
     */
    @Transactional
    public void deleteProcessMapping(String processInstanceId) {
        log.info("Deleting process mapping: {}", processInstanceId);
        
        ProcessMappingEntity mapping = findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new ProcessMappingNotFoundException(processInstanceId));
        
        processMappingRepo.delete(mapping);
        
        log.info("Process mapping deleted: {}", processInstanceId);
    }
    
    // ==================== DTOs ====================
    
    public static class ProcessStatistics {
        private final long total;
        private final long running;
        private final long completed;
        private final long failed;
        private final long cancelled;
        
        public ProcessStatistics(long total, long running, long completed, long failed, long cancelled) {
            this.total = total;
            this.running = running;
            this.completed = completed;
            this.failed = failed;
            this.cancelled = cancelled;
        }
        
        public long getTotal() { return total; }
        public long getRunning() { return running; }
        public long getCompleted() { return completed; }
        public long getFailed() { return failed; }
        public long getCancelled() { return cancelled; }
    }
    
    public static class ProcessInfo {
        private final String id;
        private final String processDefinitionKey;
        private final ProcessStatus status;
        private final LocalDateTime startedAt;
        private final LocalDateTime completedAt;
        
        public ProcessInfo(String id, String processDefinitionKey, ProcessStatus status, 
                          LocalDateTime startedAt, LocalDateTime completedAt) {
            this.id = id;
            this.processDefinitionKey = processDefinitionKey;
            this.status = status;
            this.startedAt = startedAt;
            this.completedAt = completedAt;
        }
        
        public String getId() { return id; }
        public String getProcessDefinitionKey() { return processDefinitionKey; }
        public ProcessStatus getStatus() { return status; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
    }
    
    public static class ProcessSummary {
        private final String caseId;
        private final ProcessStatistics statistics;
        private final List<ProcessInfo> processes;
        
        public ProcessSummary(String caseId, ProcessStatistics statistics, List<ProcessInfo> processes) {
            this.caseId = caseId;
            this.statistics = statistics;
            this.processes = processes;
        }
        
        public String getCaseId() { return caseId; }
        public ProcessStatistics getStatistics() { return statistics; }
        public List<ProcessInfo> getProcesses() { return processes; }
    }
}
