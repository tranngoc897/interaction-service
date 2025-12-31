-- =============================================================================
-- 1. CASE MANAGEMENT (Cấu trúc Hồ sơ)
-- =============================================================================

-- Định nghĩa các loại Case (Blueprint)
CREATE TABLE IF NOT EXISTS flw_case_def (
    case_definition_key VARCHAR(255) PRIMARY KEY,
    case_definition_version BIGINT NOT NULL,
    default_value JSONB,
    case_schema JSONB
);

-- Bảng Case thực tế (Instances)
CREATE TABLE IF NOT EXISTS flw_case (
    id UUID PRIMARY KEY,
    version INTEGER DEFAULT 0,
    case_definition_key VARCHAR(255),
    case_definition_version VARCHAR(50),
    customer_id VARCHAR(64),
    current_step VARCHAR(128),
    status VARCHAR(32),
    workflow_instance_id VARCHAR(128),
    case_data JSONB,           -- Chứa dữ liệu nghiệp vụ tổng hợp
    audit_trail JSONB,         -- Chứa lịch sử các bước đã qua
    sla JSONB,                 -- Metadata về thời gian xử lý
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_flow_case_status ON flw_case(status);
CREATE INDEX idx_flow_case_customer ON flw_case(customer_id);
CREATE INDEX idx_flow_case_workflow ON flw_case(workflow_instance_id);


-- =============================================================================
-- 2. INTERACTION MANAGEMENT (Giao dịch người dùng)
-- =============================================================================

-- Định nghĩa các bước trong một Interaction (Luồng đi)
CREATE TABLE IF NOT EXISTS flw_int_def (
    interaction_definition_key VARCHAR(255),
    interaction_definition_version BIGINT,
    version INTEGER DEFAULT 0,
    schema_id VARCHAR(255),
    steps JSONB,               -- Cấu trúc các bước: form, next step, actions...
    PRIMARY KEY (interaction_definition_key, interaction_definition_version)
);

-- Phiên giao dịch thực tế
CREATE TABLE IF NOT EXISTS flw_int (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    user_id VARCHAR(36),
    interaction_definition_key VARCHAR(255),
    interaction_definition_version BIGINT,
    case_id VARCHAR(36),       -- Link tới flw_case.id (kiểu string để linh động)
    case_version BIGINT,
    step_name VARCHAR(255),    -- Bước hiện tại khách hàng đang ở
    step_status VARCHAR(20),   -- PENDING, COMPLETED...
    status VARCHAR(20),        -- ACTIVE, COMPLETED, CANCELLED...
    resumable BOOLEAN DEFAULT TRUE,
    temp_data JSONB,           -- Dữ liệu tạm thời của bước hiện tại
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_flw_int_case_id ON flw_int(case_id);
CREATE INDEX idx_flw_int_user_id ON flw_int(user_id);


-- =============================================================================
-- 3. TASK MANAGEMENT (Công việc thủ công/Review)
-- =============================================================================

CREATE TABLE IF NOT EXISTS flw_task (
    id UUID PRIMARY KEY,
    version INTEGER DEFAULT 0,
    case_id UUID NOT NULL REFERENCES flw_case(id), -- Ràng buộc cứng với Case
    interaction_id VARCHAR(36),
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    assignee_id VARCHAR(36),
    payload JSONB,             -- Dữ liệu hiển thị cho người duyệt
    result JSONB,              -- Kết quả phê duyệt (Approve/Reject)
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX idx_flw_task_case_id ON flw_task(case_id);
CREATE INDEX idx_flw_task_status ON flw_task(status);
CREATE INDEX idx_flw_task_assignee ON flw_task(assignee_id);


-- =============================================================================
-- 4. PROCESS MAPPING (Theo dõi Workflow Temporal)
-- =============================================================================

CREATE TABLE IF NOT EXISTS process_mapping (
    id VARCHAR(255) PRIMARY KEY,
    engine_type VARCHAR(50),      -- TEMPORAL
    process_instance_id VARCHAR(255),
    process_definition_key VARCHAR(255),
    case_id VARCHAR(255),
    user_id VARCHAR(255),
    status VARCHAR(50),           -- RUNNING, COMPLETED, FAILED
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    error_details TEXT
);

CREATE INDEX idx_proc_map_case ON process_mapping(case_id);
CREATE INDEX idx_proc_map_instance ON process_mapping(process_instance_id);

-- =============================================================================
-- 6. WORKFLOW SCHEDULE MANAGEMENT (Theo dõi Temporal Schedules)
-- =============================================================================

CREATE TABLE IF NOT EXISTS workflow_schedule (
    schedule_id VARCHAR(255) PRIMARY KEY,
    cron_expression VARCHAR(255) NOT NULL,
    workflow_type VARCHAR(255) NOT NULL,
    task_queue VARCHAR(255),
    created_by VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    workflow_arguments TEXT
);

CREATE INDEX idx_schedule_id ON workflow_schedule(schedule_id);
CREATE INDEX idx_schedule_status ON workflow_schedule(status);
CREATE INDEX idx_schedule_created_by ON workflow_schedule(created_by);

-- =============================================================================
-- 5. TRIGGER CẬP NHẬT THỜI GIAN (PostgreSQL)
-- =============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_update_flow_case_updated_at
    BEFORE UPDATE ON flw_case
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
