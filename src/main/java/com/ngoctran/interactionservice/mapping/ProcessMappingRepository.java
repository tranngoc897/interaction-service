package com.ngoctran.interactionservice.mapping;

import com.ngoctran.interactionservice.mapping.enums.EngineType;
import com.ngoctran.interactionservice.mapping.enums.ProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessMappingRepository extends JpaRepository<ProcessMappingEntity, String> {
    
    Optional<ProcessMappingEntity> findByProcessInstanceId(String processInstanceId);
    
    List<ProcessMappingEntity> findByCaseId(String caseId);
    
    List<ProcessMappingEntity> findByCaseIdAndStatus(String caseId, ProcessStatus status);
    
    List<ProcessMappingEntity> findByUserId(String userId);
    
    List<ProcessMappingEntity> findByEngineType(EngineType engineType);
    
    List<ProcessMappingEntity> findByStatus(ProcessStatus status);
    
    List<ProcessMappingEntity> findByProcessDefinitionKey(String processDefinitionKey);
    
    @Query("SELECT p FROM ProcessMappingEntity p WHERE p.caseId = :caseId AND p.status = com.ngoctran.interactionservice.mapping.enums.ProcessStatus.RUNNING")
    List<ProcessMappingEntity> findRunningProcessesByCaseId(@Param("caseId") String caseId);
    
    @Query("SELECT p FROM ProcessMappingEntity p WHERE p.caseId = :caseId AND p.status = com.ngoctran.interactionservice.mapping.enums.ProcessStatus.COMPLETED")
    List<ProcessMappingEntity> findCompletedProcessesByCaseId(@Param("caseId") String caseId);
    
    @Query("SELECT p FROM ProcessMappingEntity p WHERE p.status = com.ngoctran.interactionservice.mapping.enums.ProcessStatus.FAILED AND p.completedAt >= :since")
    List<ProcessMappingEntity> findFailedProcessesSince(@Param("since") LocalDateTime since);
    
    long countByCaseIdAndStatus(String caseId, ProcessStatus status);
    
    boolean existsByCaseIdAndProcessDefinitionKey(String caseId, String processDefinitionKey);
    
    Optional<ProcessMappingEntity> findFirstByCaseIdAndProcessDefinitionKeyOrderByStartedAtDesc(
            String caseId, 
            String processDefinitionKey
    );
    
    @Query("SELECT p FROM ProcessMappingEntity p WHERE p.startedAt BETWEEN :start AND :end")
    List<ProcessMappingEntity> findProcessesStartedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    
    @Query("SELECT p FROM ProcessMappingEntity p WHERE p.status = com.ngoctran.interactionservice.mapping.enums.ProcessStatus.RUNNING AND p.startedAt < :threshold")
    List<ProcessMappingEntity> findLongRunningProcesses(@Param("threshold") LocalDateTime threshold);
}

