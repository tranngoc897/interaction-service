package com.ngoctran.interactionservice.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, UUID> {
    List<CaseEntity> findByCustomerId(String customerId);

    List<CaseEntity> findByStatus(String status);

    List<CaseEntity> findByCustomerIdAndStatus(String customerId, String status);
}
