-- Migration: Add SAGA compensation support
-- Date: 2026-01-09
-- Description: Add compensation_action column to support SAGA pattern rollback

ALTER TABLE onboarding_transition 
ADD COLUMN IF NOT EXISTS compensation_action VARCHAR(100);

COMMENT ON COLUMN onboarding_transition.compensation_action IS 'SAGA: Action to undo this transition';

-- Example data: Define compensation actions for critical steps
UPDATE onboarding_transition 
SET compensation_action = 'UNDO_ACCOUNT_CREATION' 
WHERE to_state = 'ACCOUNT_CREATED' AND compensation_action IS NULL;

UPDATE onboarding_transition 
SET compensation_action = 'UNDO_WALLET_CREATION' 
WHERE to_state = 'WALLET_CREATED' AND compensation_action IS NULL;

UPDATE onboarding_transition 
SET compensation_action = 'UNDO_CARD_ACTIVATION' 
WHERE to_state = 'CARD_ACTIVATED' AND compensation_action IS NULL;
