-- Sample data for onboarding workflow transitions
-- This defines the state machine for the onboarding process

-- Clear existing data (for development)
DELETE FROM onboarding_transition WHERE flow_version = 'v1';

-- Insert transition definitions with conditions
INSERT INTO onboarding_transition (flow_version, from_state, action, to_state, is_async, source_service, allowed_actors, max_retry, conditions_json) VALUES
-- Phone entry and OTP
('v1', 'PHONE_ENTERED', 'NEXT', 'OTP_VERIFIED', false, 'UI', ARRAY['USER'], 3, '[]'),
('v1', 'OTP_VERIFIED', 'NEXT', 'PROFILE_COMPLETED', false, 'UI', ARRAY['USER'], 3, '["otp_status == SUCCESS"]'),

-- Profile and documents
('v1', 'PROFILE_COMPLETED', 'NEXT', 'DOC_UPLOADED', false, 'UI', ARRAY['USER'], 3, '[]'),
('v1', 'DOC_UPLOADED', 'NEXT', 'EKYC_PENDING', true, 'UI', ARRAY['USER'], 3, '[]'),

-- eKYC async process
('v1', 'EKYC_PENDING', 'EKYC_CALLBACK_OK', 'EKYC_APPROVED', false, 'EKYC', ARRAY['SYSTEM', 'KAFKA'], 3, '["ekyc_score >= 0.8"]'),
('v1', 'EKYC_PENDING', 'EKYC_CALLBACK_FAIL', 'EKYC_REJECTED', false, 'EKYC', ARRAY['SYSTEM', 'KAFKA'], 3, '[]'),
('v1', 'EKYC_PENDING', 'TIMEOUT', 'EKYC_TIMEOUT', false, 'SYSTEM', ARRAY['SYSTEM'], 3, '[]'),

-- eKYC approved to AML
('v1', 'EKYC_APPROVED', 'NEXT', 'AML_PENDING', true, 'UI', ARRAY['USER'], 3, '[]'),

-- AML async process
('v1', 'AML_PENDING', 'AML_CALLBACK_OK', 'AML_CLEARED', false, 'AML', ARRAY['SYSTEM', 'KAFKA'], 3, '["aml_status == CLEAR"]'),
('v1', 'AML_PENDING', 'AML_CALLBACK_FAIL', 'AML_REJECTED', false, 'AML', ARRAY['SYSTEM', 'KAFKA'], 3, '[]'),
('v1', 'AML_PENDING', 'TIMEOUT', 'AML_TIMEOUT', false, 'SYSTEM', ARRAY['SYSTEM'], 3, '[]'),

-- Final steps
('v1', 'AML_CLEARED', 'NEXT', 'ACCOUNT_CREATED', false, 'UI', ARRAY['USER'], 3, '[]'),
('v1', 'ACCOUNT_CREATED', 'NEXT', 'COMPLETED', false, 'UI', ARRAY['USER'], 3, '[]'),

-- Retry actions (same state)
('v1', 'EKYC_PENDING', 'RETRY', 'EKYC_PENDING', true, 'SYSTEM', ARRAY['SYSTEM', 'ADMIN'], 3, '[]'),
('v1', 'AML_PENDING', 'RETRY', 'AML_PENDING', true, 'SYSTEM', ARRAY['SYSTEM', 'ADMIN'], 3, '[]');

-- Sample onboarding instance for testing
-- INSERT INTO onboarding_instance (id, user_id, flow_version, current_state, status, version) VALUES
-- ('550e8400-e29b-41d4-a716-446655440000', 'user123', 'v1', 'PHONE_ENTERED', 'ACTIVE', 0);
