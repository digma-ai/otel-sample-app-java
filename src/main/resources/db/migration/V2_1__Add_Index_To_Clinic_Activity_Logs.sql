-- Add index to clinic_activity_logs table for numeric_value column
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_numeric_value ON clinic_activity_logs(numeric_value);