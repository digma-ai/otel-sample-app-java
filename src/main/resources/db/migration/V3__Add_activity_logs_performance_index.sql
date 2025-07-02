-- Add index for improved activity logs query performance
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_timestamp_type 
ON clinic_activity_logs (event_timestamp DESC, activity_type);

-- Ensure proper statistics are gathered
ANALYZE clinic_activity_logs;