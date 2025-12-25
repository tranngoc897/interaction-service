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
