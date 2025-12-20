package com.ngoctran.interactionservice.interaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InteractionDefinitionRepository extends JpaRepository<InteractionDefinitionEntity, String> {
    
    Optional<InteractionDefinitionEntity> findByInteractionDefinitionKeyAndInteractionDefinitionVersion(
            String key, 
            Long version
    );
}
