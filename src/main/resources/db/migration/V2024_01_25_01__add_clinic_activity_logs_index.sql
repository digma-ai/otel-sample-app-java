-- Add index to improve performance of numeric_value queries
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_numeric_value ON clinic_activity_logs(numeric_value);