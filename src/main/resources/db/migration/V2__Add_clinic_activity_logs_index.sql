-- Add index on numeric_value column to optimize query performance
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_numeric_value ON clinic_activity_logs(numeric_value);

-- Add statistics gathering to help query planner
ANALYZE clinic_activity_logs;