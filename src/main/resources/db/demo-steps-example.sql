-- ============================================================================
-- DEMO: 3 Types of "steps" in Interaction Service
-- ============================================================================
-- This script demonstrates:
-- 1. flw_int_def.steps (JSONB) - BLUEPRINT: Step definitions
-- 2. flw_int.step_name - CURRENT POSITION: Where user is now
-- 3. flow_case.audit_trail (JSONB) - HISTORY: Steps completed
-- ============================================================================

-- ============================================================================
-- 1. BLUEPRINT: Define the journey template (flw_int_def.steps)
-- ============================================================================

-- Insert interaction definition with step blueprint
INSERT INTO flw_int_def (
    interaction_definition_key,
    interaction_definition_version,
    case_definition_key,
    case_definition_version,
    schema_id,
    steps
) VALUES (
    'kyc-onboarding',
    1,
    'customer-profile',
    1,
    'kyc-schema-v1',
    '[
        {
            "name": "welcome",
            "type": "info",
            "title": "Chào mừng đến với ABC Bank",
            "description": "Chúng tôi cần 5 phút để xác minh thông tin của bạn",
            "next": "personal-info",
            "uiSchema": {
                "component": "WelcomeScreen",
                "showProgress": true
            }
        },
        {
            "name": "personal-info",
            "type": "form",
            "title": "Thông tin cá nhân",
            "description": "Vui lòng cung cấp thông tin cá nhân của bạn",
            "fields": [
                {
                    "name": "fullName",
                    "type": "text",
                    "label": "Họ và tên",
                    "required": true,
                    "placeholder": "Nguyễn Văn A"
                },
                {
                    "name": "dob",
                    "type": "date",
                    "label": "Ngày sinh",
                    "required": true
                },
                {
                    "name": "idNumber",
                    "type": "text",
                    "label": "Số CMND/CCCD",
                    "required": true,
                    "validation": {
                        "pattern": "^[0-9]{9,12}$",
                        "message": "Số CMND/CCCD không hợp lệ"
                    }
                }
            ],
            "validation": {
                "minAge": 18
            },
            "next": "address-info",
            "onSubmit": [
                {
                    "action": "validateWithService",
                    "service": "id-verification-service"
                }
            ],
            "uiSchema": {
                "component": "FormScreen",
                "layout": "vertical"
            }
        },
        {
            "name": "address-info",
            "type": "form",
            "title": "Địa chỉ liên hệ",
            "description": "Vui lòng cung cấp địa chỉ hiện tại của bạn",
            "fields": [
                {
                    "name": "street",
                    "type": "text",
                    "label": "Số nhà, tên đường",
                    "required": true
                },
                {
                    "name": "city",
                    "type": "select",
                    "label": "Thành phố",
                    "required": true,
                    "options": [
                        {"value": "HCM", "label": "Hồ Chí Minh"},
                        {"value": "HN", "label": "Hà Nội"},
                        {"value": "DN", "label": "Đà Nẵng"}
                    ]
                },
                {
                    "name": "district",
                    "type": "select",
                    "label": "Quận/Huyện",
                    "required": true
                }
            ],
            "next": "document-upload",
            "uiSchema": {
                "component": "AddressForm"
            }
        },
        {
            "name": "document-upload",
            "type": "upload",
            "title": "Tải lên giấy tờ",
            "description": "Vui lòng tải lên ảnh CMND/CCCD và ảnh chân dung",
            "documents": [
                {
                    "type": "id-front",
                    "label": "CMND/CCCD mặt trước",
                    "required": true
                },
                {
                    "type": "id-back",
                    "label": "CMND/CCCD mặt sau",
                    "required": true
                },
                {
                    "type": "selfie",
                    "label": "Ảnh chân dung",
                    "required": true
                }
            ],
            "next": "review",
            "onSubmit": [
                {
                    "action": "startWorkflow",
                    "onboarding": "ocr-verification-onboarding"
                }
            ],
            "uiSchema": {
                "component": "DocumentUpload",
                "maxFileSize": "5MB",
                "acceptedFormats": ["jpg", "png", "pdf"]
            }
        },
        {
            "name": "review",
            "type": "review",
            "title": "Xác nhận thông tin",
            "description": "Vui lòng kiểm tra lại thông tin trước khi gửi",
            "showSummary": true,
            "next": "waiting-approval",
            "onSubmit": [
                {
                    "action": "startWorkflow",
                    "onboarding": "kyc-approval-onboarding"
                }
            ],
            "uiSchema": {
                "component": "ReviewScreen",
                "allowEdit": true
            }
        },
        {
            "name": "waiting-approval",
            "type": "waiting",
            "title": "Đang xử lý",
            "description": "Chúng tôi đang xác minh thông tin của bạn. Vui lòng chờ trong giây lát.",
            "estimatedTime": "2-3 phút",
            "resumable": true,
            "next": "completed",
            "uiSchema": {
                "component": "WaitingScreen",
                "showSpinner": true,
                "allowClose": true
            }
        },
        {
            "name": "completed",
            "type": "result",
            "title": "Hoàn tất!",
            "description": "Tài khoản của bạn đã được kích hoạt thành công!",
            "uiSchema": {
                "component": "SuccessScreen",
                "showConfetti": true
            }
        }
    ]'::jsonb
);

-- Insert case definition
INSERT INTO flw_case_def (
    case_definition_key,
    case_definition_version,
    default_value,
    case_schema
) VALUES (
    'customer-profile',
    1,
    '{}'::jsonb,
    '{
        "type": "object",
        "properties": {
            "fullName": {"type": "string"},
            "dob": {"type": "string", "format": "date"},
            "idNumber": {"type": "string"},
            "street": {"type": "string"},
            "city": {"type": "string"},
            "district": {"type": "string"},
            "documents": {
                "type": "object",
                "properties": {
                    "idFront": {"type": "string"},
                    "idBack": {"type": "string"},
                    "selfie": {"type": "string"}
                }
            }
        }
    }'::jsonb
);

-- ============================================================================
-- 2. RUNTIME INSTANCES: User journey in progress
-- ============================================================================

-- Scenario 1: User just started, at "welcome" step
INSERT INTO flow_case (
    id,
    customer_id,
    current_step,
    status,
    case_data,
    audit_trail,
    version,
    case_definition_key,
    case_definition_version,
    created_at,
    updated_at
) VALUES (
    'c1111111-1111-1111-1111-111111111111',
    'user-001',
    'welcome',
    'IN_PROGRESS',
    '{}'::jsonb,
    '{"steps": [], "lastUpdated": "2025-12-20T08:00:00Z"}'::jsonb,
    1,
    'customer-profile',
    '1',
    NOW(),
    NOW()
);

INSERT INTO flw_int (
    id,
    version,
    user_id,
    interaction_definition_key,
    interaction_definition_version,
    case_definition_key,
    case_definition_version,
    case_id,
    case_version,
    step_name,           -- ← CURRENT POSITION
    step_status,
    status,
    resumable,
    temp_data
) VALUES (
    'int-001',
    1,
    'user-001',
    'kyc-onboarding',
    1,
    'customer-profile',
    1,
    'c1111111-1111-1111-1111-111111111111',
    1,
    'welcome',           -- ← User is at "welcome" step
    'PENDING',
    'ACTIVE',
    true,
    '{}'::jsonb
);

-- Scenario 2: User at "address-info" step (has completed 2 steps)
INSERT INTO flow_case (
    id,
    customer_id,
    current_step,
    status,
    case_data,
    audit_trail,
    version,
    case_definition_key,
    case_definition_version,
    created_at,
    updated_at
) VALUES (
    'c2222222-2222-2222-2222-222222222222',
    'user-002',
    'address-info',
    'IN_PROGRESS',
    '{
        "fullName": "Nguyen Van A",
        "dob": "1990-01-01",
        "idNumber": "123456789"
    }'::jsonb,
    '{
        "steps": [
            {
                "stepName": "welcome",
                "status": "COMPLETED",
                "startedAt": "2025-12-20T08:00:00Z",
                "completedAt": "2025-12-20T08:00:30Z",
                "data": {},
                "metadata": {
                    "userAgent": "Mozilla/5.0",
                    "ipAddress": "192.168.1.100"
                }
            },
            {
                "stepName": "personal-info",
                "status": "COMPLETED",
                "startedAt": "2025-12-20T08:00:30Z",
                "completedAt": "2025-12-20T08:05:00Z",
                "data": {
                    "fullName": "Nguyen Van A",
                    "dob": "1990-01-01",
                    "idNumber": "123456789"
                },
                "metadata": {
                    "userAgent": "Mozilla/5.0",
                    "ipAddress": "192.168.1.100"
                }
            }
        ],
        "lastUpdated": "2025-12-20T08:05:00Z"
    }'::jsonb,
    1,
    'customer-profile',
    '1',
    NOW() - INTERVAL '5 minutes',
    NOW()
);

INSERT INTO flw_int (
    id,
    version,
    user_id,
    interaction_definition_key,
    interaction_definition_version,
    case_definition_key,
    case_definition_version,
    case_id,
    case_version,
    step_name,           -- ← CURRENT POSITION
    step_status,
    status,
    resumable,
    temp_data
) VALUES (
    'int-002',
    1,
    'user-002',
    'kyc-onboarding',
    1,
    'customer-profile',
    1,
    'c2222222-2222-2222-2222-222222222222',
    1,
    'address-info',      -- ← User is at "address-info" step
    'PENDING',
    'ACTIVE',
    true,
    '{}'::jsonb
);

-- Scenario 3: User waiting for system approval
INSERT INTO flow_case (
    id,
    customer_id,
    current_step,
    status,
    workflow_instance_id,
    case_data,
    audit_trail,
    version,
    case_definition_key,
    case_definition_version,
    created_at,
    updated_at
) VALUES (
    'c3333333-3333-3333-3333-333333333333',
    'user-003',
    'waiting-approval',
    'WAITING_APPROVAL',
    'wf-kyc-approval-12345',
    '{
        "fullName": "Tran Thi B",
        "dob": "1995-05-15",
        "idNumber": "987654321",
        "street": "123 Nguyen Hue",
        "city": "HCM",
        "district": "District 1",
        "documents": {
            "idFront": "s3://bucket/id-front-123.jpg",
            "idBack": "s3://bucket/id-back-123.jpg",
            "selfie": "s3://bucket/selfie-123.jpg"
        }
    }'::jsonb,
    '{
        "steps": [
            {
                "stepName": "welcome",
                "status": "COMPLETED",
                "completedAt": "2025-12-20T07:00:00Z",
                "data": {}
            },
            {
                "stepName": "personal-info",
                "status": "COMPLETED",
                "completedAt": "2025-12-20T07:05:00Z",
                "data": {
                    "fullName": "Tran Thi B",
                    "dob": "1995-05-15",
                    "idNumber": "987654321"
                }
            },
            {
                "stepName": "address-info",
                "status": "COMPLETED",
                "completedAt": "2025-12-20T07:10:00Z",
                "data": {
                    "street": "123 Nguyen Hue",
                    "city": "HCM",
                    "district": "District 1"
                }
            },
            {
                "stepName": "document-upload",
                "status": "COMPLETED",
                "completedAt": "2025-12-20T07:15:00Z",
                "data": {
                    "idFront": "s3://bucket/id-front-123.jpg",
                    "idBack": "s3://bucket/id-back-123.jpg",
                    "selfie": "s3://bucket/selfie-123.jpg"
                }
            },
            {
                "stepName": "review",
                "status": "COMPLETED",
                "completedAt": "2025-12-20T07:20:00Z",
                "data": {}
            }
        ],
        "lastUpdated": "2025-12-20T07:20:00Z"
    }'::jsonb,
    1,
    'customer-profile',
    '1',
    NOW() - INTERVAL '1 hour',
    NOW()
);

INSERT INTO flw_int (
    id,
    version,
    user_id,
    interaction_definition_key,
    interaction_definition_version,
    case_definition_key,
    case_definition_version,
    case_id,
    case_version,
    step_name,           -- ← CURRENT POSITION
    step_status,
    status,
    resumable,
    temp_data
) VALUES (
    'int-003',
    1,
    'user-003',
    'kyc-onboarding',
    1,
    'customer-profile',
    1,
    'c3333333-3333-3333-3333-333333333333',
    1,
    'waiting-approval',  -- ← User is waiting for system
    'COMPLETED',
    'WAITING_SYSTEM',
    true,
    '{}'::jsonb
);

INSERT INTO flw_process_mapping (
    id,
    engine_type,
    process_instance_id,
    process_definition_key,
    business_key,
    case_id,
    user_id,
    status,
    started_at,
    created_at,
    updated_at
) VALUES (
    'pm-003',
    'TEMPORAL',
    'wf-kyc-approval-12345',
    'kyc-approval-onboarding',
    'user-003-kyc',
    'c3333333-3333-3333-3333-333333333333',
    'user-003',
    'RUNNING',
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '5 minutes',
    NOW()
);

-- ============================================================================
-- QUERY EXAMPLES: How to use the 3 types of "steps"
-- ============================================================================

-- Example 1: Get BLUEPRINT (all possible steps in the journey)
SELECT 
    interaction_definition_key,
    interaction_definition_version,
    jsonb_array_length(steps) as total_steps,
    steps
FROM flw_int_def
WHERE interaction_definition_key = 'kyc-onboarding';

-- Example 2: Get CURRENT POSITION (where is user now?)
SELECT 
    i.id as interaction_id,
    i.user_id,
    i.step_name as current_step,          -- ← CURRENT POSITION
    i.step_status,
    i.status as interaction_status
FROM flw_int i
WHERE i.user_id = 'user-002';

-- Example 3: Get HISTORY (what steps has user completed?)
SELECT 
    c.id as case_id,
    c.customer_id,
    c.current_step,
    c.audit_trail->'steps' as step_history  -- ← HISTORY
FROM flow_case c
WHERE c.customer_id = 'user-002';

-- Example 4: Combine all 3 - Full picture of user's journey
SELECT 
    i.id as interaction_id,
    i.user_id,
    i.step_name as current_step,                    -- ← CURRENT POSITION
    i.step_status,
    def.steps as all_steps_blueprint,               -- ← BLUEPRINT
    c.audit_trail->'steps' as completed_steps,      -- ← HISTORY
    c.case_data as collected_data
FROM flw_int i
JOIN flw_int_def def 
    ON def.interaction_definition_key = i.interaction_definition_key
    AND def.interaction_definition_version = i.interaction_definition_version
JOIN flow_case c 
    ON c.id::text = i.case_id
WHERE i.user_id = 'user-002';

-- Example 5: Get specific step definition from blueprint
SELECT 
    interaction_definition_key,
    step_def->>'name' as step_name,
    step_def->>'type' as step_type,
    step_def->>'title' as step_title,
    step_def->'fields' as form_fields
FROM flw_int_def,
     jsonb_array_elements(steps) as step_def
WHERE interaction_definition_key = 'kyc-onboarding'
  AND step_def->>'name' = 'personal-info';

-- Example 6: Analytics - How many users at each step?
SELECT 
    step_name as current_step,
    COUNT(*) as user_count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM flw_int
WHERE interaction_definition_key = 'kyc-onboarding'
  AND status = 'ACTIVE'
GROUP BY step_name
ORDER BY user_count DESC;
