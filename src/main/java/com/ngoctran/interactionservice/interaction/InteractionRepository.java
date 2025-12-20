package com.ngoctran.interactionservice.interaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for InteractionEntity
 * 
 * Supports 1:N relationship with CaseEntity:
 * - One Case can have many Interactions
 * - Query interactions by case ID
 */
@Repository
public interface InteractionRepository extends JpaRepository<InteractionEntity, String> {
    
    /**
     * Find all interactions for a specific case
     * Use case: Get all journeys/sessions for a customer's case
     */
    List<InteractionEntity> findByCaseId(String caseId);
    
    /**
     * Find interactions by case ID and status
     * Use case: Get only active/completed interactions for a case
     */
    List<InteractionEntity> findByCaseIdAndStatus(String caseId, String status);
    
    /**
     * Find interactions by user ID
     * Use case: Get all interactions for a user across all cases
     */
    List<InteractionEntity> findByUserId(String userId);
    
    /**
     * Find interactions by user ID and status
     */
    List<InteractionEntity> findByUserIdAndStatus(String userId, String status);
    
    /**
     * Count interactions for a case
     * Use case: Analytics - how many times has this case been interacted with?
     */
    long countByCaseId(String caseId);
    
    /**
     * Find the latest interaction for a case
     * Use case: Resume the most recent journey
     */
    Optional<InteractionEntity> findFirstByCaseIdOrderByIdDesc(String caseId);
    
    /**
     * Find active interaction for a case
     * Use case: Check if there's an ongoing journey for this case
     */
    Optional<InteractionEntity> findFirstByCaseIdAndStatusIn(String caseId, List<String> statuses);
    
    /**
     * Custom query: Get interaction summary for a case
     */
    @Query("SELECT i FROM InteractionEntity i WHERE i.caseId = :caseId ORDER BY i.id DESC")
    List<InteractionEntity> findInteractionsByCaseIdOrderByIdDesc(@Param("caseId") String caseId);
}