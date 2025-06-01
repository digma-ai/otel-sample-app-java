-- Add index to improve query performance on numeric_value column
CREATE INDEX idx_clinic_activity_logs_numeric_value ON clinic_activity_logs(numeric_value);