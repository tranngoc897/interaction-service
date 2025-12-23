package com.ngoctran.interactionservice.workflow.activity;

import com.ngoctran.interactionservice.interaction.InteractionEntity;
import com.ngoctran.interactionservice.interaction.InteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupActivityImpl implements CleanupActivity {

    private final InteractionRepository interactionRepo;

    @Override
    public int cleanupstaleInteractions(int hoursOld) {
        log.info("Starting cleanup of interactions older than {} hours", hoursOld);

        Instant threshold = Instant.now().minus(hoursOld, ChronoUnit.HOURS);

        // Find active interactions that haven't been updated
        List<InteractionEntity> staleInteractions = interactionRepo
                .findByStatusAndUpdatedAtBefore("ACTIVE", threshold);

        log.info("Found {} stale interactions to cancel", staleInteractions.size());

        for (InteractionEntity interaction : staleInteractions) {
            log.info("Cancelling stale interaction: {}", interaction.getId());
            interaction.setStatus("CANCELLED_STALE");
            interactionRepo.save(interaction);
        }

        return staleInteractions.size();
    }
}
