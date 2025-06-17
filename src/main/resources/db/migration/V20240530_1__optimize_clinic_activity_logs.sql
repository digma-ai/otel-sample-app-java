-- Add covering index for better query performance
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_covering ON clinic_activity_logs 
(numeric_value) 
INCLUDE (id, activity_type, event_timestamp, status_flag, payload);

-- Drop old index as it's now redundant
DROP INDEX IF EXISTS idx_clinic_activity_logs_numeric_value;

-- Update table statistics
ANALYZE clinic_activity_logs;