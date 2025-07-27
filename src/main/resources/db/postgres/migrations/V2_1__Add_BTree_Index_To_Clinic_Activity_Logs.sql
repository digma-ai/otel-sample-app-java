-- Add B-tree index on numeric_value column to improve query performance
-- Migration Version: 2.1
-- Description: Adds an index to optimize queries filtering or sorting by numeric_value

-- Up Migration
CREATE INDEX idx_clinic_activity_logs_numeric_value ON clinic_activity_logs USING btree (numeric_value);

-- Down Migration
-- To rollback, uncomment and execute:
-- DROP INDEX idx_clinic_activity_logs_numeric_value;