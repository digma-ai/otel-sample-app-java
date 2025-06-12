-- Add index on numeric_value column to improve query performance
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_numeric_value ON clinic_activity_logs(numeric_value);

-- Add comment to document the index
COMMENT ON INDEX idx_clinic_activity_logs_numeric_value IS 'Index added to improve performance of queries filtering on numeric_value column. Related incident: 5aa1fb7a-3656-11f0-8db3-ca59c7e8e81d';