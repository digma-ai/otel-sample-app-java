-- Add index to improve query performance
CREATE INDEX IF NOT EXISTS idx_clinic_activity_numeric_value ON clinic_activity_logs(numeric_value);

-- Install pg_stat_statements extension for query monitoring
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;