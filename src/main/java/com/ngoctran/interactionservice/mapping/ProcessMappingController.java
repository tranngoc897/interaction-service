package com.ngoctran.interactionservice.mapping;

import com.ngoctran.interactionservice.mapping.dto.ProcessMappingMapper;
import com.ngoctran.interactionservice.mapping.dto.ProcessMappingResponse;
import com.ngoctran.interactionservice.mapping.enums.ProcessStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for Process Mapping operations
 * 
 * Provides APIs for querying and managing process mappings
 */
@RestController
@RequestMapping("/api/process-mappings")
@RequiredArgsConstructor
public class ProcessMappingController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProcessMappingController.class);
    
    private final ProcessMappingService processMappingService;
    
    /**
     * Get all process mappings for a case
     */
    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<ProcessMappingResponse>> getProcessMappingsByCaseId(
            @PathVariable String caseId) {
        
        log.info("Getting process mappings for case: {}", caseId);
        
        List<ProcessMappingResponse> mappings = processMappingService.getProcessMappingsByCaseId(caseId)
                .stream()
                .map(ProcessMappingMapper::toResponse)
                .toList();
        
        return ResponseEntity.ok(mappings);
    }
    
    /**
     * Get running processes for a case
     */
    @GetMapping("/case/{caseId}/running")
    public ResponseEntity<List<ProcessMappingResponse>> getRunningProcessesByCaseId(
            @PathVariable String caseId) {
        
        log.info("Getting running processes for case: {}", caseId);
        
        List<ProcessMappingResponse> mappings = processMappingService.getRunningProcessesByCaseId(caseId)
                .stream()
                .map(ProcessMappingMapper::toResponse)
                .toList();
        
        return ResponseEntity.ok(mappings);
    }
    
    /**
     * Get completed processes for a case
     */
    @GetMapping("/case/{caseId}/completed")
    public ResponseEntity<List<ProcessMappingResponse>> getCompletedProcessesByCaseId(
            @PathVariable String caseId) {
        
        log.info("Getting completed processes for case: {}", caseId);
        
        List<ProcessMappingResponse> mappings = processMappingService.getCompletedProcessesByCaseId(caseId)
                .stream()
                .map(ProcessMappingMapper::toResponse)
                .toList();
        
        return ResponseEntity.ok(mappings);
    }
    
    /**
     * Get process statistics for a case
     */
    @GetMapping("/case/{caseId}/statistics")
    public ResponseEntity<ProcessMappingService.ProcessStatistics> getProcessStatistics(
            @PathVariable String caseId) {
        
        log.info("Getting process statistics for case: {}", caseId);
        
        ProcessMappingService.ProcessStatistics stats = processMappingService.getProcessStatistics(caseId);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get process summary for a case
     */
    @GetMapping("/case/{caseId}/summary")
    public ResponseEntity<ProcessMappingService.ProcessSummary> getProcessSummary(
            @PathVariable String caseId) {
        
        log.info("Getting process summary for case: {}", caseId);
        
        ProcessMappingService.ProcessSummary summary = processMappingService.getProcessSummary(caseId);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Check if case has running process
     */
    @GetMapping("/case/{caseId}/has-running")
    public ResponseEntity<Boolean> hasRunningProcess(@PathVariable String caseId) {
        log.info("Checking if case has running process: {}", caseId);
        
        boolean hasRunning = processMappingService.hasRunningProcess(caseId);
        
        return ResponseEntity.ok(hasRunning);
    }
    
    /**
     * Get process mapping by process instance ID
     */
    @GetMapping("/process/{processInstanceId}")
    public ResponseEntity<ProcessMappingResponse> getProcessMapping(
            @PathVariable String processInstanceId) {
        
        log.info("Getting process mapping: {}", processInstanceId);
        
        return processMappingService.findByProcessInstanceId(processInstanceId)
                .map(ProcessMappingMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all process mappings for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProcessMappingResponse>> getProcessMappingsByUserId(
            @PathVariable String userId) {
        
        log.info("Getting process mappings for user: {}", userId);
        
        List<ProcessMappingResponse> mappings = processMappingService.getProcessMappingsByUserId(userId)
                .stream()
                .map(ProcessMappingMapper::toResponse)
                .toList();
        
        return ResponseEntity.ok(mappings);
    }
    
    /**
     * Get processes by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProcessMappingResponse>> getProcessMappingsByStatus(
            @PathVariable String status) {
        
        log.info("Getting process mappings by status: {}", status);
        
        ProcessStatus processStatus = ProcessStatus.valueOf(status.toUpperCase());
        List<ProcessMappingResponse> mappings = processMappingService.getProcessMappingsByStatus(processStatus)
                .stream()
                .map(ProcessMappingMapper::toResponse)
                .toList();
        
        return ResponseEntity.ok(mappings);
    }
    
    /**
     * Get failed processes since a specific time
     */
    @GetMapping("/failed")
    public ResponseEntity<List<ProcessMappingResponse>> getFailedProcesses(
            @RequestParam(required = false, defaultValue = "24") int hoursAgo) {
        
        log.info("Getting failed processes from last {} hours", hoursAgo);
        
        LocalDateTime since = LocalDateTime.now().minusHours(hoursAgo);
        List<ProcessMappingResponse> mappings = processMappingService.getFailedProcessesSince(since)
                .stream()
                .map(ProcessMappingMapper::toResponse)
                .toList();
        
        return ResponseEntity.ok(mappings);
    }
    
    /**
     * Get long-running processes
     */
    @GetMapping("/long-running")
    public ResponseEntity<List<ProcessMappingResponse>> getLongRunningProcesses(
            @RequestParam(required = false, defaultValue = "24") int hoursThreshold) {
        
        log.info("Getting processes running longer than {} hours", hoursThreshold);
        
        Duration duration = Duration.ofHours(hoursThreshold);
        List<ProcessMappingResponse> mappings = processMappingService.getLongRunningProcesses(duration)
                .stream()
                .map(ProcessMappingMapper::toResponse)
                .toList();
        
        return ResponseEntity.ok(mappings);
    }
    
    /**
     * Mark process as completed
     */
    @PostMapping("/process/{processInstanceId}/complete")
    public ResponseEntity<Void> markProcessCompleted(@PathVariable String processInstanceId) {
        log.info("Marking process as completed: {}", processInstanceId);
        
        processMappingService.markProcessCompleted(processInstanceId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Mark process as failed
     */
    @PostMapping("/process/{processInstanceId}/fail")
    public ResponseEntity<Void> markProcessFailed(
            @PathVariable String processInstanceId,
            @RequestParam String errorMessage) {
        
        log.info("Marking process as failed: {}", processInstanceId);
        
        processMappingService.markProcessFailed(processInstanceId, errorMessage);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Mark process as cancelled
     */
    @PostMapping("/process/{processInstanceId}/cancel")
    public ResponseEntity<Void> markProcessCancelled(@PathVariable String processInstanceId) {
        log.info("Marking process as cancelled: {}", processInstanceId);
        
        processMappingService.markProcessCancelled(processInstanceId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Delete process mapping
     */
    @DeleteMapping("/process/{processInstanceId}")
    public ResponseEntity<Void> deleteProcessMapping(@PathVariable String processInstanceId) {
        log.info("Deleting process mapping: {}", processInstanceId);
        
        processMappingService.deleteProcessMapping(processInstanceId);
        
        return ResponseEntity.ok().build();
    }
}
