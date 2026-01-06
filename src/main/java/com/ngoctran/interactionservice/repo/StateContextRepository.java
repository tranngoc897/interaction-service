package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.StateContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StateContextRepository extends JpaRepository<StateContext, UUID> {
}
