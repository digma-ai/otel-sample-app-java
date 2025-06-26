-- Add index on numeric_value column for performance optimization
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_numeric_value ON clinic_activity_logs(numeric_value);