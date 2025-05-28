-- Add index for numeric_value column to improve basic filtering
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_numeric_value 
ON clinic_activity_logs(numeric_value);

-- Add compound index for common query patterns
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_type_value 
ON clinic_activity_logs(activity_type, numeric_value);

-- Add index including timestamp for time-based queries
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_value_timestamp 
ON clinic_activity_logs(numeric_value, event_timestamp DESC);