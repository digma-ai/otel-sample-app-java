-- Add index on numeric_value column for improved query performance
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_numeric_value 
ON clinic_activity_logs(numeric_value);

-- Rollback statement (will be used by Flyway when rolling back this migration)
-- DROP INDEX IF EXISTS idx_clinic_activity_logs_numeric_value;