-- =============================================================================
-- CUSTOM WORKFLOW ENGINE SCHEMA (No Camunda)
-- Based on state machine + transition table + event-driven architecture
-- =============================================================================

-- =============================================================================
-- 1. ONBOARDING INSTANCE (Main workflow instance)
-- =============================================================================
CREATE TABLE IF NOT EXISTS onboarding_instance (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    flow_version VARCHAR(10) NOT NULL DEFAULT 'v1',
    current_state VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, CANCELLED, FAILED
    version BIGINT NOT NULL DEFAULT 0, -- Optimistic locking
    state_started_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_onboarding_instance_user ON onboarding_instance(user_id);
CREATE INDEX idx_onboarding_instance_state ON onboarding_instance(current_state);
CREATE INDEX idx_onboarding_instance_status ON onboarding_instance(status);

-- =============================================================================
-- 2. TRANSITION TABLE (Workflow definition - config-driven)
-- =============================================================================
CREATE TABLE IF NOT EXISTS onboarding_transition (
    flow_version VARCHAR(10),
    from_state VARCHAR(50),
    action VARCHAR(50),
    to_state VARCHAR(50),
    is_async BOOLEAN DEFAULT FALSE,
    source_service VARCHAR(50), -- EKYC, AML, UI, SYSTEM
    allowed_actors TEXT[], -- USER, ADMIN, RISK, SYSTEM
    max_retry INTEGER DEFAULT 3,
    conditions_json JSONB, -- Rule conditions like ["otp_status == SUCCESS"]
    PRIMARY KEY (flow_version, from_state, action)
);

CREATE INDEX idx_transition_lookup ON onboarding_transition(flow_version, from_state, action);

-- =============================================================================
-- 3. ONBOARDING HISTORY (Audit trail)
-- =============================================================================
CREATE TABLE IF NOT EXISTS onboarding_history (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL REFERENCES onboarding_instance(id),
    from_state VARCHAR(50),
    to_state VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    result VARCHAR(20) NOT NULL, -- SUCCESS, FAILED
    error_code VARCHAR(50),
    error_message TEXT,
    actor VARCHAR(20), -- USER, ADMIN, SYSTEM, KAFKA
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_history_instance ON onboarding_history(instance_id);
CREATE INDEX idx_history_created ON onboarding_history(created_at);

-- =============================================================================
-- 4. STEP EXECUTION (Retry & execution tracking)
-- =============================================================================
CREATE TABLE IF NOT EXISTS step_execution (
    instance_id UUID,
    state VARCHAR(50),
    status VARCHAR(20) NOT NULL, -- NEW, RUNNING, SUCCESS, FAILED
    retry_count INTEGER DEFAULT 0,
    max_retry INTEGER DEFAULT 3,
    last_error_code VARCHAR(50),
    last_error_message TEXT,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (instance_id, state)
);

CREATE INDEX idx_step_execution_retry ON step_execution(status, next_retry_at);
CREATE INDEX idx_step_execution_instance ON step_execution(instance_id);

-- =============================================================================
-- 5. PROCESSED EVENTS (Idempotency for Kafka/UI actions)
-- =============================================================================
CREATE TABLE IF NOT EXISTS processed_event (
    event_id VARCHAR(255) PRIMARY KEY,
    instance_id UUID,
    event_type VARCHAR(50),
    processed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_event_instance ON processed_event(instance_id);

-- =============================================================================
-- 6. HUMAN TASK (Manual review tasks)
-- =============================================================================
CREATE TABLE IF NOT EXISTS human_task (
    task_id UUID PRIMARY KEY,
    instance_id UUID NOT NULL REFERENCES onboarding_instance(id),
    state VARCHAR(50) NOT NULL,
    task_type VARCHAR(50) NOT NULL, -- AML_REVIEW, EKYC_REVIEW, MANUAL_APPROVAL
    assigned_role VARCHAR(50), -- RISK_OFFICER, COMPLIANCE_OFFICER
    assigned_user VARCHAR(64),
    status VARCHAR(20) NOT NULL, -- OPEN, CLAIMED, COMPLETED
    priority VARCHAR(10) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, CRITICAL
    payload JSONB, -- Task data for display
    result JSONB, -- Approval result
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ -- SLA deadline
);

CREATE INDEX idx_human_task_instance ON human_task(instance_id);
CREATE INDEX idx_human_task_status ON human_task(status);
CREATE INDEX idx_human_task_assigned ON human_task(assigned_role, assigned_user);
CREATE INDEX idx_human_task_due ON human_task(due_at);

-- =============================================================================
-- 7. INCIDENT (Error management & escalation)
-- =============================================================================
CREATE TABLE IF NOT EXISTS incident (
    incident_id UUID PRIMARY KEY,
    instance_id UUID NOT NULL REFERENCES onboarding_instance(id),
    state VARCHAR(50),
    error_code VARCHAR(50) NOT NULL,
    severity VARCHAR(10) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    status VARCHAR(20) NOT NULL, -- OPEN, ACKNOWLEDGED, RESOLVED
    owner VARCHAR(64), -- Assigned person
    description TEXT,
    resolution TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_incident_instance ON incident(instance_id);
CREATE INDEX idx_incident_status ON incident(status);
CREATE INDEX idx_incident_severity ON incident(severity);

-- =============================================================================
-- 8. DLQ EVENTS (Dead Letter Queue for failed Kafka events)
-- =============================================================================
CREATE TABLE IF NOT EXISTS dlq_event (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    partition_key VARCHAR(255),
    event_payload JSONB NOT NULL,
    error_message TEXT,
    failed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'NEW' -- NEW, RETRIED, IGNORED
);

CREATE INDEX idx_dlq_topic ON dlq_event(topic);
CREATE INDEX idx_dlq_status ON dlq_event(status);

-- =============================================================================
-- 9. WORKFLOW METRICS (For monitoring)
-- =============================================================================
CREATE TABLE IF NOT EXISTS workflow_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    state VARCHAR(50),
    value BIGINT,
    tags JSONB,
    recorded_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_metrics_name ON workflow_metrics(metric_name);
CREATE INDEX idx_metrics_recorded ON workflow_metrics(recorded_at);

-- =============================================================================
-- 10. STATE CONTEXT (Workflow data for rule evaluation)
-- =============================================================================
CREATE TABLE IF NOT EXISTS state_context (
    instance_id UUID PRIMARY KEY,
    context_data JSONB, -- Workflow data like {"otp_status": "SUCCESS", "ekyc_score": 0.85}
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- =============================================================================
-- 11. STATE SNAPSHOTS (For debugging failed workflows)
-- =============================================================================
CREATE TABLE IF NOT EXISTS state_snapshot (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    state VARCHAR(50) NOT NULL,
    snapshot_data JSONB, -- Complete state snapshot
    context_data JSONB, -- Context at time of snapshot
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_state_snapshot_instance ON state_snapshot(instance_id);

-- =============================================================================
-- 12. OUTBOX EVENTS (Guaranteed event delivery)
-- =============================================================================
CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    topic VARCHAR(100) NOT NULL,
    partition_key VARCHAR(255),
    event_payload JSONB NOT NULL,
    event_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PUBLISHED, FAILED
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status ON outbox_event(status);
CREATE INDEX idx_outbox_created ON outbox_event(created_at);

-- =============================================================================
-- TRIGGERS FOR UPDATED_AT
-- =============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_update_onboarding_instance
    BEFORE UPDATE ON onboarding_instance
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_update_step_execution
    BEFORE UPDATE ON step_execution
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
