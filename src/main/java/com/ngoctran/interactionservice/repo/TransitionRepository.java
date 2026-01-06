package com.ngoctran.interactionservice.repo;

import com.ngoctran.interactionservice.domain.Transition;
import com.ngoctran.interactionservice.domain.TransitionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransitionRepository extends JpaRepository<Transition, TransitionId> {

    List<Transition> findByFlowVersion(String flowVersion);

    List<Transition> findByFlowVersionAndFromState(String flowVersion, String fromState);

    @Query("SELECT t FROM Transition t WHERE t.flowVersion = :flowVersion AND t.fromState = :fromState AND t.action = :action")
    Transition findByFlowVersionAndFromStateAndAction(
            @Param("flowVersion") String flowVersion,
            @Param("fromState") String fromState,
            @Param("action") String action
    );
}
