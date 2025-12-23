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
 * Handles the lifecycle of process mappings between business processes and
 * onboarding engines
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessMappingService {

    private final ProcessMappingRepository processMappingRepo;
    private final ObjectMapper objectMapper;

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
                null);
    }

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
        mapping.setCaseId(UUID.fromString(caseId));
        mapping.setUserId(userId);
        mapping.setStatus(ProcessStatus.RUNNING);
        mapping.setStartedAt(LocalDateTime.now());

        // Note: businessKey and metadata are currently not supported by the existing
        // table schema
        // and are ignored in this implementation to prevent schema-validation errors.

        return processMappingRepo.save(mapping);
    }

    @Transactional
    public void markProcessCompleted(String processInstanceId) {
        ProcessMappingEntity mapping = findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new ProcessMappingNotFoundException(processInstanceId));
        mapping.markCompleted();
        processMappingRepo.save(mapping);
    }

    @Transactional
    public void markProcessFailed(String processInstanceId, String errorMessage) {
        ProcessMappingEntity mapping = findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new ProcessMappingNotFoundException(processInstanceId));
        mapping.markFailed(errorMessage);
        processMappingRepo.save(mapping);
    }

    @Transactional
    public void markProcessCancelled(String processInstanceId) {
        ProcessMappingEntity mapping = findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new ProcessMappingNotFoundException(processInstanceId));
        mapping.markCancelled();
        processMappingRepo.save(mapping);
    }

    @Transactional
    public void updateProcessMetadata(String processInstanceId, Map<String, Object> metadata) {
        // Metadata is currently not supported by the existing table schema.
        log.warn("updateProcessMetadata called but metadata column is missing in DB schema.");
    }

    public Optional<ProcessMappingEntity> findByProcessInstanceId(String processInstanceId) {
        return processMappingRepo.findByProcessInstanceId(processInstanceId);
    }

    public List<ProcessMappingEntity> getProcessMappingsByCaseId(String caseId) {
        return processMappingRepo.findByCaseId(UUID.fromString(caseId));
    }

    public List<ProcessMappingEntity> getRunningProcessesByCaseId(String caseId) {
        return processMappingRepo.findRunningProcessesByCaseId(UUID.fromString(caseId));
    }

    public List<ProcessMappingEntity> getCompletedProcessesByCaseId(String caseId) {
        return processMappingRepo.findCompletedProcessesByCaseId(UUID.fromString(caseId));
    }

    public boolean hasRunningProcess(String caseId) {
        return processMappingRepo.countByCaseIdAndStatus(UUID.fromString(caseId), ProcessStatus.RUNNING) > 0;
    }

    public Optional<ProcessMappingEntity> getLatestProcess(String caseId, String processDefinitionKey) {
        return processMappingRepo.findFirstByCaseIdAndProcessDefinitionKeyOrderByStartedAtDesc(
                UUID.fromString(caseId),
                processDefinitionKey);
    }

    public List<ProcessMappingEntity> getProcessMappingsByUserId(String userId) {
        return processMappingRepo.findByUserId(userId);
    }

    public List<ProcessMappingEntity> getProcessMappingsByStatus(ProcessStatus status) {
        return processMappingRepo.findByStatus(status);
    }

    public List<ProcessMappingEntity> getFailedProcessesSince(LocalDateTime since) {
        return processMappingRepo.findFailedProcessesSince(since);
    }

    public List<ProcessMappingEntity> getLongRunningProcesses(Duration duration) {
        LocalDateTime threshold = LocalDateTime.now().minus(duration);
        return processMappingRepo.findLongRunningProcesses(threshold);
    }

    public ProcessStatistics getProcessStatistics(String caseId) {
        List<ProcessMappingEntity> allProcesses = processMappingRepo.findByCaseId(UUID.fromString(caseId));

        long total = allProcesses.size();
        long running = allProcesses.stream().filter(ProcessMappingEntity::isRunning).count();
        long completed = allProcesses.stream().filter(ProcessMappingEntity::isCompleted).count();
        long failed = allProcesses.stream().filter(ProcessMappingEntity::isFailed).count();
        long cancelled = allProcesses.stream().filter(ProcessMappingEntity::isCancelled).count();

        return new ProcessStatistics(total, running, completed, failed, cancelled);
    }

    public ProcessSummary getProcessSummary(String caseId) {
        List<ProcessMappingEntity> processes = processMappingRepo.findByCaseId(UUID.fromString(caseId));
        ProcessStatistics stats = getProcessStatistics(caseId);

        List<ProcessInfo> processInfos = processes.stream()
                .map(p -> new ProcessInfo(
                        p.getId(),
                        p.getProcessDefinitionKey(),
                        p.getStatus(),
                        p.getStartedAt(),
                        p.getCompletedAt()))
                .collect(Collectors.toList());

        return new ProcessSummary(caseId, stats, processInfos);
    }

    @Transactional
    public void deleteProcessMapping(String processInstanceId) {
        ProcessMappingEntity mapping = findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new ProcessMappingNotFoundException(processInstanceId));
        processMappingRepo.delete(mapping);
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

        public long getTotal() {
            return total;
        }

        public long getRunning() {
            return running;
        }

        public long getCompleted() {
            return completed;
        }

        public long getFailed() {
            return failed;
        }

        public long getCancelled() {
            return cancelled;
        }
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

        public String getId() {
            return id;
        }

        public String getProcessDefinitionKey() {
            return processDefinitionKey;
        }

        public ProcessStatus getStatus() {
            return status;
        }

        public LocalDateTime getStartedAt() {
            return startedAt;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }
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

        public String getCaseId() {
            return caseId;
        }

        public ProcessStatistics getStatistics() {
            return statistics;
        }

        public List<ProcessInfo> getProcesses() {
            return processes;
        }
    }
}
