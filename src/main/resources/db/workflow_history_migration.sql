-- Migration script for Workflow History table
-- Execute this script to create the workflow_history table in PostgreSQL

-- Create workflow_history table
CREATE TABLE IF NOT EXISTS workflow_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id VARCHAR(255) NOT NULL,
    workflow_type VARCHAR(100),
    action VARCHAR(100),
    status_before VARCHAR(50),
    status_after VARCHAR(50),
    changed_by VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_details JSONB,
    reason TEXT,
    notes TEXT,
    ip_address VARCHAR(45), -- Support both IPv4 and IPv6
    user_agent TEXT,
    session_id VARCHAR(255),
    metadata JSONB,
    version BIGINT DEFAULT 0
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_workflow_history_workflow_id ON workflow_history(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_history_workflow_type ON workflow_history(workflow_type);
CREATE INDEX IF NOT EXISTS idx_workflow_history_changed_by ON workflow_history(changed_by);
CREATE INDEX IF NOT EXISTS idx_workflow_history_action ON workflow_history(action);
CREATE INDEX IF NOT EXISTS idx_workflow_history_status_before ON workflow_history(status_before);
CREATE INDEX IF NOT EXISTS idx_workflow_history_status_after ON workflow_history(status_after);
CREATE INDEX IF NOT EXISTS idx_workflow_history_changed_at ON workflow_history(changed_at);
CREATE INDEX IF NOT EXISTS idx_workflow_history_ip_address ON workflow_history(ip_address);

-- Create GIN indexes for JSONB columns for efficient JSON queries
CREATE INDEX IF NOT EXISTS idx_workflow_history_change_details ON workflow_history USING GIN(change_details);
CREATE INDEX IF NOT EXISTS idx_workflow_history_metadata ON workflow_history USING GIN(metadata);

-- Create composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_workflow_history_workflow_action ON workflow_history(workflow_id, action);
CREATE INDEX IF NOT EXISTS idx_workflow_history_workflow_date ON workflow_history(workflow_id, changed_at);
CREATE INDEX IF NOT EXISTS idx_workflow_history_type_date ON workflow_history(workflow_type, changed_at DESC);

-- Add comments for documentation
COMMENT ON TABLE workflow_history IS 'Audit trail for all workflow state changes and actions';
COMMENT ON COLUMN workflow_history.history_id IS 'Unique identifier for each history entry';
COMMENT ON COLUMN workflow_history.workflow_id IS 'ID of the workflow this history belongs to';
COMMENT ON COLUMN workflow_history.workflow_type IS 'Type of workflow (KYC_ONBOARDING, PAYMENT, RECONCILIATION, etc.)';
COMMENT ON COLUMN workflow_history.action IS 'Action performed (START, SIGNAL, COMPLETE, FAIL, CANCEL, etc.)';
COMMENT ON COLUMN workflow_history.status_before IS 'Workflow status before this action';
COMMENT ON COLUMN workflow_history.status_after IS 'Workflow status after this action';
COMMENT ON COLUMN workflow_history.changed_by IS 'User or system that performed the action';
COMMENT ON COLUMN workflow_history.changed_at IS 'Timestamp when the action was performed';
COMMENT ON COLUMN workflow_history.change_details IS 'Detailed information about the change (JSON)';
COMMENT ON COLUMN workflow_history.reason IS 'Reason for the change';
COMMENT ON COLUMN workflow_history.notes IS 'Additional notes about the change';
COMMENT ON COLUMN workflow_history.ip_address IS 'IP address of the user who performed the action';
COMMENT ON COLUMN workflow_history.user_agent IS 'User agent string from the request';
COMMENT ON COLUMN workflow_history.session_id IS 'Session ID for tracking user sessions';
COMMENT ON COLUMN workflow_history.metadata IS 'Additional metadata for querying (JSON)';
COMMENT ON COLUMN workflow_history.version IS 'Version for optimistic locking';

-- Create a view for recent workflow activity (last 24 hours)
CREATE OR REPLACE VIEW recent_workflow_activity AS
SELECT
    workflow_id,
    workflow_type,
    action,
    status_before,
    status_after,
    changed_by,
    changed_at,
    reason
FROM workflow_history
WHERE changed_at >= CURRENT_TIMESTAMP - INTERVAL '24 hours'
ORDER BY changed_at DESC;

-- Create a view for workflow failure analysis
CREATE OR REPLACE VIEW workflow_failure_analysis AS
SELECT
    workflow_id,
    workflow_type,
    action,
    status_before,
    status_after,
    changed_by,
    changed_at,
    change_details->>'error' as error_message,
    change_details->>'errorDetails' as error_details,
    reason
FROM workflow_history
WHERE action = 'FAILURE' AND status_after = 'FAILED'
ORDER BY changed_at DESC;

-- Grant permissions (adjust as needed for your security model)
-- GRANT SELECT, INSERT ON workflow_history TO your_app_user;
-- GRANT SELECT ON recent_workflow_activity TO your_app_user;
-- GRANT SELECT ON workflow_failure_analysis TO your_app_user;

-- ===========================================
-- ABB-ONBOARDING PATTERN MIGRATIONS
-- ===========================================

-- Enhance existing flw_case_def table with ABB patterns
ALTER TABLE flw_case_def ADD COLUMN IF NOT EXISTS name VARCHAR(255);
ALTER TABLE flw_case_def ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE flw_case_def ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE flw_case_def ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE flw_case_def ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE flw_case_def ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE flw_case_def ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE flw_case_def ADD COLUMN IF NOT EXISTS interaction_flow_json TEXT; -- For ABB complex flows
ALTER TABLE flw_case_def ADD COLUMN IF NOT EXISTS simple_steps_json TEXT; -- For existing simple UI flows

-- Enhance existing flw_case table with ABB patterns
ALTER TABLE flw_case ADD COLUMN IF NOT EXISTS resume_token TEXT; -- For workflow resume/pause
ALTER TABLE flw_case ADD COLUMN IF NOT EXISTS workflow_state TEXT; -- JSON state for resumability
ALTER TABLE flw_case ADD COLUMN IF NOT EXISTS epic_data TEXT; -- JSON epic/milestone tracking
ALTER TABLE flw_case ADD COLUMN IF NOT EXISTS compliance_status TEXT; -- JSON compliance check results
ALTER TABLE flw_case ADD COLUMN IF NOT EXISTS joint_account_data TEXT; -- JSON joint account info
ALTER TABLE flw_case ADD COLUMN IF NOT EXISTS bpmn_process_id VARCHAR(255); -- BPMN process instance ID
ALTER TABLE flw_case ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP; -- For cleanup

-- Create indexes for case_definitions
CREATE INDEX IF NOT EXISTS idx_case_definitions_key ON case_definitions(key);
CREATE INDEX IF NOT EXISTS idx_case_definitions_status ON case_definitions(status);
CREATE INDEX IF NOT EXISTS idx_case_definitions_updated_at ON case_definitions(updated_at);

-- Create indexes for interaction_definitions
CREATE INDEX IF NOT EXISTS idx_interaction_definitions_key ON interaction_definitions(key);
CREATE INDEX IF NOT EXISTS idx_interaction_definitions_case_key ON interaction_definitions(case_definition_key);
CREATE INDEX IF NOT EXISTS idx_interaction_definitions_status ON interaction_definitions(status);
CREATE INDEX IF NOT EXISTS idx_interaction_definitions_updated_at ON interaction_definitions(updated_at);

-- Create indexes for epics
CREATE INDEX IF NOT EXISTS idx_epics_key ON epics(key);
CREATE INDEX IF NOT EXISTS idx_epics_case_definition_key ON epics(case_definition_key);
CREATE INDEX IF NOT EXISTS idx_epics_status ON epics(status);

-- Create indexes for milestones
CREATE INDEX IF NOT EXISTS idx_milestones_case_id ON milestones(case_id);
CREATE INDEX IF NOT EXISTS idx_milestones_epic_key ON milestones(epic_key);
CREATE INDEX IF NOT EXISTS idx_milestones_milestone_key ON milestones(milestone_key);
CREATE INDEX IF NOT EXISTS idx_milestones_status ON milestones(status);
CREATE INDEX IF NOT EXISTS idx_milestones_created_at ON milestones(created_at);
CREATE INDEX IF NOT EXISTS idx_milestones_case_epic ON milestones(case_id, epic_key);
CREATE INDEX IF NOT EXISTS idx_milestones_case_status ON milestones(case_id, status);

-- Add foreign key constraints (optional, depending on your data integrity needs)
-- ALTER TABLE interaction_definitions ADD CONSTRAINT fk_interaction_case_definition
--     FOREIGN KEY (case_definition_key) REFERENCES case_definitions(key);
-- ALTER TABLE epics ADD CONSTRAINT fk_epic_case_definition
--     FOREIGN KEY (case_definition_key) REFERENCES case_definitions(key);
-- ALTER TABLE milestones ADD CONSTRAINT fk_milestone_epic
--     FOREIGN KEY (epic_key) REFERENCES epics(key);

-- Add comments for documentation
COMMENT ON TABLE case_definitions IS 'JSON Schema-based case definitions for dynamic case management';
COMMENT ON TABLE interaction_definitions IS 'JSON-based interaction flow definitions for step orchestration';
COMMENT ON TABLE epics IS 'Epic definitions for milestone tracking system';
COMMENT ON TABLE milestones IS 'Milestone progress tracking within epics';

-- Create views for monitoring
CREATE OR REPLACE VIEW case_definition_summary AS
SELECT
    key,
    name,
    version,
    status,
    created_by,
    updated_at
FROM case_definitions
ORDER BY updated_at DESC;

CREATE OR REPLACE VIEW milestone_progress AS
SELECT
    case_id,
    epic_key,
    milestone_key,
    name,
    status,
    started_at,
    completed_at,
    EXTRACT(EPOCH FROM (completed_at - started_at))/3600 as duration_hours
FROM milestones
WHERE status = 'COMPLETED'
ORDER BY completed_at DESC;

CREATE OR REPLACE VIEW active_milestones AS
SELECT
    case_id,
    epic_key,
    milestone_key,
    name,
    started_at,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - started_at))/3600 as hours_running
FROM milestones
WHERE status = 'IN_PROGRESS'
ORDER BY started_at ASC;

-- Create workflow_states table (for resume/pause functionality)
CREATE TABLE IF NOT EXISTS workflow_states (
    id BIGSERIAL PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    workflow_type VARCHAR(100) NOT NULL,
    current_step VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    workflow_data TEXT,
    step_context TEXT,
    resume_token TEXT,
    paused_at TIMESTAMP,
    resumed_at TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days'),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(workflow_id)
);

-- Create indexes for workflow_states
CREATE INDEX IF NOT EXISTS idx_workflow_states_workflow_id ON workflow_states(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_states_workflow_type ON workflow_states(workflow_type);
CREATE INDEX IF NOT EXISTS idx_workflow_states_status ON workflow_states(status);
CREATE INDEX IF NOT EXISTS idx_workflow_states_expires_at ON workflow_states(expires_at);
CREATE INDEX IF NOT EXISTS idx_workflow_states_created_at ON workflow_states(created_at);
CREATE INDEX IF NOT EXISTS idx_workflow_states_type_status ON workflow_states(workflow_type, status);

-- Add comments for workflow_states
COMMENT ON TABLE workflow_states IS 'Workflow state persistence for resume/pause functionality';
COMMENT ON COLUMN workflow_states.workflow_id IS 'Unique workflow identifier';
COMMENT ON COLUMN workflow_states.workflow_type IS 'Type of workflow (ONBOARDING, KYC, etc.)';
COMMENT ON COLUMN workflow_states.current_step IS 'Current step in the workflow';
COMMENT ON COLUMN workflow_states.resume_token IS 'Token for secure workflow resumption';

-- Create views for workflow state monitoring
CREATE OR REPLACE VIEW paused_workflows AS
SELECT
    workflow_id,
    workflow_type,
    current_step,
    paused_at,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - paused_at))/3600 as hours_paused
FROM workflow_states
WHERE status = 'PAUSED'
ORDER BY paused_at DESC;

CREATE OR REPLACE VIEW workflow_state_summary AS
SELECT
    workflow_type,
    status,
    COUNT(*) as count
FROM workflow_states
GROUP BY workflow_type, status
ORDER BY workflow_type, status;

-- Create compliance_checks table (enhanced AML/KYC compliance)
CREATE TABLE IF NOT EXISTS compliance_checks (
    id BIGSERIAL PRIMARY KEY,
    case_id VARCHAR(255) NOT NULL,
    applicant_id VARCHAR(255) NOT NULL,
    check_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    check_data TEXT,
    check_result TEXT,
    risk_level VARCHAR(20),
    manual_review_required BOOLEAN,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '90 days'),
    audit_trail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for compliance_checks
CREATE INDEX IF NOT EXISTS idx_compliance_checks_case_id ON compliance_checks(case_id);
CREATE INDEX IF NOT EXISTS idx_compliance_checks_applicant_id ON compliance_checks(applicant_id);
CREATE INDEX IF NOT EXISTS idx_compliance_checks_check_type ON compliance_checks(check_type);
CREATE INDEX IF NOT EXISTS idx_compliance_checks_status ON compliance_checks(status);
CREATE INDEX IF NOT EXISTS idx_compliance_checks_expires_at ON compliance_checks(expires_at);
CREATE INDEX IF NOT EXISTS idx_compliance_checks_created_at ON compliance_checks(created_at);
CREATE INDEX IF NOT EXISTS idx_compliance_checks_manual_review ON compliance_checks(manual_review_required, status);

-- Add comments for compliance_checks
COMMENT ON TABLE compliance_checks IS 'Enhanced AML/KYC compliance checking and audit trails';
COMMENT ON COLUMN compliance_checks.case_id IS 'Reference to the case this check belongs to';
COMMENT ON COLUMN compliance_checks.applicant_id IS 'Reference to the applicant being checked';
COMMENT ON COLUMN compliance_checks.check_type IS 'Type of compliance check (AML, KYC, SANCTIONS, etc.)';
COMMENT ON COLUMN compliance_checks.audit_trail IS 'JSON audit trail of all actions on this check';

-- Create joint_accounts table (joint account/co-applicant support)
CREATE TABLE IF NOT EXISTS joint_accounts (
    id BIGSERIAL PRIMARY KEY,
    case_id VARCHAR(255) NOT NULL,
    primary_applicant_id VARCHAR(255) NOT NULL,
    co_applicant_id VARCHAR(255) NOT NULL,
    relationship_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    invitation_sent_at TIMESTAMP,
    invitation_accepted_at TIMESTAMP,
    co_applicant_joined_at TIMESTAMP,
    invitation_token TEXT,
    shared_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(case_id, co_applicant_id)
);

-- Create indexes for joint_accounts
CREATE INDEX IF NOT EXISTS idx_joint_accounts_case_id ON joint_accounts(case_id);
CREATE INDEX IF NOT EXISTS idx_joint_accounts_primary_applicant ON joint_accounts(primary_applicant_id);
CREATE INDEX IF NOT EXISTS idx_joint_accounts_co_applicant ON joint_accounts(co_applicant_id);
CREATE INDEX IF NOT EXISTS idx_joint_accounts_status ON joint_accounts(status);
CREATE INDEX IF NOT EXISTS idx_joint_accounts_invitation_token ON joint_accounts(invitation_token);

-- Add comments for joint_accounts
COMMENT ON TABLE joint_accounts IS 'Joint account relationships and co-applicant management';
COMMENT ON COLUMN joint_accounts.invitation_token IS 'Secure token for co-applicant invitation';
COMMENT ON COLUMN joint_accounts.shared_data IS 'JSON data shared between applicants';

-- Create views for compliance monitoring
CREATE OR REPLACE VIEW compliance_check_summary AS
SELECT
    case_id,
    applicant_id,
    check_type,
    status,
    risk_level,
    manual_review_required,
    created_at
FROM compliance_checks
ORDER BY created_at DESC;

CREATE OR REPLACE VIEW compliance_failure_analysis AS
SELECT
    case_id,
    applicant_id,
    check_type,
    status,
    risk_level,
    check_result,
    created_at
FROM compliance_checks
WHERE status IN ('FAILED', 'REVIEW_NEEDED')
ORDER BY created_at DESC;

CREATE OR REPLACE VIEW joint_account_summary AS
SELECT
    case_id,
    primary_applicant_id,
    co_applicant_id,
    relationship_type,
    status,
    invitation_sent_at,
    invitation_accepted_at
FROM joint_accounts
ORDER BY created_at DESC;
