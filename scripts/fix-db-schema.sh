#!/bin/bash
# Quick fix: Add missing columns to database
# Run this script if you want to quickly update the database without migration

echo "🔧 Adding missing columns to database..."

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "❌ psql not found. Please install PostgreSQL client."
    exit 1
fi

# Database connection details (adjust as needed)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-workflow_db}"
DB_USER="${DB_USER:-postgres}"

echo "📊 Connecting to database: $DB_NAME@$DB_HOST:$DB_PORT"

# Add compensation_action column
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
-- Add SAGA compensation column
ALTER TABLE onboarding_transition 
ADD COLUMN IF NOT EXISTS compensation_action VARCHAR(100);

-- Add workflow versioning column
ALTER TABLE workflow_event 
ADD COLUMN IF NOT EXISTS code_version INT DEFAULT 1;

-- Update existing events
UPDATE workflow_event 
SET code_version = 1 
WHERE code_version IS NULL;

-- Verify changes
SELECT 
    'onboarding_transition' as table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_name = 'onboarding_transition' 
  AND column_name = 'compensation_action'
UNION ALL
SELECT 
    'workflow_event' as table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_name = 'workflow_event' 
  AND column_name = 'code_version';
EOF

if [ $? -eq 0 ]; then
    echo "✅ Database updated successfully!"
    echo "📝 Columns added:"
    echo "   - onboarding_transition.compensation_action"
    echo "   - workflow_event.code_version"
else
    echo "❌ Failed to update database"
    exit 1
fi
