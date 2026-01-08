package com.ngoctran.interactionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get the visual graph representation of the workflow
     */
    public Map<String, Object> getWorkflowGraph(String flowVersion) {
        String sql = "SELECT from_state, to_state, action, allowed_actors, is_async FROM onboarding_transition WHERE flow_version = ?";

        List<Map<String, Object>> transitions = jdbcTemplate.queryForList(sql, flowVersion);

        return Map.of(
                "version", flowVersion,
                "transitions", transitions,
                "nodes", extractUniqueStates(transitions));
    }

    private List<String> extractUniqueStates(List<Map<String, Object>> transitions) {
        return transitions.stream()
                .flatMap(t -> java.util.stream.Stream.of((String) t.get("from_state"), (String) t.get("to_state")))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }
}
