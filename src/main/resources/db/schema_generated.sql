-- =============================================================================
-- GENERATED SCHEMA SCRIPT
-- =============================================================================

-- 1. INTERACTION ENTITY (flw_int)
CREATE TABLE IF NOT EXISTS flw_int (
    id VARCHAR(255) PRIMARY KEY,
    version BIGINT,
    user_id VARCHAR(36),
    interaction_definition_key VARCHAR(255),
    interaction_definition_version BIGINT,
    case_id UUID,
    step_name VARCHAR(255),
    step_status VARCHAR(20),
    status VARCHAR(20),
    resumable BOOLEAN,
    updated_at TIMESTAMPTZ,
    temp_data JSONB
);

CREATE INDEX IF NOT EXISTS idx_flw_int_case_id ON flw_int(case_id);
CREATE INDEX IF NOT EXISTS idx_flw_int_user_id ON flw_int(user_id);

-- 2. WORKFLOW HISTORY (workflow_history)
CREATE TABLE IF NOT EXISTS workflow_history (
    history_id UUID PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    workflow_type VARCHAR(255),
    action VARCHAR(255),
    status_before VARCHAR(50),
    status VARCHAR(50),
    status_after VARCHAR(50),
    changed_by VARCHAR(255),
    changed_at TIMESTAMPTZ NOT NULL,
    change_details JSONB,
    reason VARCHAR(255),
    notes VARCHAR(255),
    ip_address VARCHAR(255),
    user_agent VARCHAR(255),
    session_id VARCHAR(255),
    metadata JSONB,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_workflow_history_workflow_id ON workflow_history(workflow_id);

-- 3. STATE CONTEXT (state_context)
CREATE TABLE IF NOT EXISTS state_context (
    instance_id UUID PRIMARY KEY,
    context_data JSONB,
    updated_at TIMESTAMPTZ,
    version BIGINT
);

-- 4. STEP EXECUTION (step_execution)
CREATE TABLE IF NOT EXISTS step_execution (
    instance_id UUID,
    state VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    retry_count INTEGER,
    max_retry INTEGER,
    last_error_code VARCHAR(255),
    last_error_message VARCHAR(255),
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    PRIMARY KEY (instance_id, state)
);

CREATE INDEX IF NOT EXISTS idx_step_execution_status ON step_execution(status);

-- 5. OUTBOX EVENT (outbox_event)
CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    topic VARCHAR(255) NOT NULL,
    partition_key VARCHAR(255),
    event_payload JSONB NOT NULL,
    event_type VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    retry_count INTEGER,
    created_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_event_status ON outbox_event(status);

-- 6. TRANSITION (onboarding_transition)
CREATE TABLE IF NOT EXISTS onboarding_transition (
    flow_version VARCHAR(255),
    from_state VARCHAR(255),
    action VARCHAR(255),
    to_state VARCHAR(255),
    is_async BOOLEAN,
    source_service VARCHAR(255),
    allowed_actors TEXT[],
    max_retry INTEGER,
    conditions_json JSONB,
    PRIMARY KEY (flow_version, from_state, action)
);

-- 7. INCIDENT (incident)
CREATE TABLE IF NOT EXISTS incident (
    incident_id UUID PRIMARY KEY,
    instance_id UUID NOT NULL,
    state VARCHAR(255) NOT NULL,
    error_code VARCHAR(255) NOT NULL,
    severity VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    owner VARCHAR(255),
    description TEXT,
    resolution TEXT,
    created_at TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_incident_instance_id ON incident(instance_id);

-- 8. HUMAN TASK (human_task)
CREATE TABLE IF NOT EXISTS human_task (
    task_id UUID PRIMARY KEY,
    instance_id UUID NOT NULL,
    state VARCHAR(255) NOT NULL,
    task_type VARCHAR(255) NOT NULL,
    assigned_role VARCHAR(255),
    assigned_user VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    priority VARCHAR(255),
    payload JSONB,
    result JSONB,
    created_at TIMESTAMPTZ,
    claimed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_human_task_instance_id ON human_task(instance_id);

-- 9. WORKFLOW METRICS (workflow_metrics)
CREATE TABLE IF NOT EXISTS workflow_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    state VARCHAR(255),
    value BIGINT NOT NULL,
    tags JSONB,
    recorded_at TIMESTAMPTZ
);

-- 10. ONBOARDING INSTANCE (onboarding_instance)
CREATE TABLE IF NOT EXISTS onboarding_instance (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    flow_version VARCHAR(255) NOT NULL,
    current_state VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    version BIGINT,
    state_started_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_onboarding_instance_user_id ON onboarding_instance(user_id);

-- 11. DLQ EVENT (dlq_event)
CREATE TABLE IF NOT EXISTS dlq_event (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    partition_key VARCHAR(255),
    event_payload JSONB NOT NULL,
    error_message TEXT,
    failed_at TIMESTAMPTZ,
    retry_count INTEGER,
    status VARCHAR(255) NOT NULL
);

-- 12. PROCESSED EVENT (processed_event)
CREATE TABLE IF NOT EXISTS processed_event (
    event_id VARCHAR(255) PRIMARY KEY,
    instance_id UUID,
    event_type VARCHAR(255),
    processed_at TIMESTAMPTZ
);

-- 13. JOINT ACCOUNTS (joint_accounts)
CREATE TABLE IF NOT EXISTS joint_accounts (
    id BIGSERIAL PRIMARY KEY,
    case_id VARCHAR(255) NOT NULL,
    primary_applicant_id VARCHAR(255) NOT NULL,
    co_applicant_id VARCHAR(255) NOT NULL,
    relationship_type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    invitation_sent_at TIMESTAMP,
    invitation_accepted_at TIMESTAMP,
    co_applicant_joined_at TIMESTAMP,
    invitation_token TEXT,
    shared_data TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_joint_accounts_case_id ON joint_accounts(case_id);

-- 14. COMPLIANCE CHECKS (compliance_checks)
CREATE TABLE IF NOT EXISTS compliance_checks (
    id BIGSERIAL PRIMARY KEY,
    case_id VARCHAR(255) NOT NULL,
    applicant_id VARCHAR(255) NOT NULL,
    check_type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    check_data TEXT,
    check_result TEXT,
    risk_level VARCHAR(255),
    manual_review_required BOOLEAN,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP,
    expires_at TIMESTAMP,
    audit_trail TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_compliance_checks_case_id ON compliance_checks(case_id);
