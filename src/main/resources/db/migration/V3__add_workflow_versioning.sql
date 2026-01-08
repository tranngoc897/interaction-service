-- Migration: Add workflow versioning support
-- Date: 2026-01-09
-- Description: Add code_version column to workflow_event for versioning support

ALTER TABLE workflow_event 
ADD COLUMN IF NOT EXISTS code_version INT DEFAULT 1;

COMMENT ON COLUMN workflow_event.code_version IS 'Track which version of code was used for this event';

-- Update existing events to version 1
UPDATE workflow_event 
SET code_version = 1 
WHERE code_version IS NULL;
