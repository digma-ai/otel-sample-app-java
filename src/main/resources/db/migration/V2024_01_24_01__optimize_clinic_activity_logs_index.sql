-- Drop existing index
DROP INDEX IF EXISTS idx_clinic_activity_logs_numeric_value;

-- Create optimized index with better fill factor for less fragmentation
CREATE INDEX idx_clinic_activity_logs_numeric_value ON clinic_activity_logs (numeric_value) WITH (fillfactor=90);

-- Update statistics
ANALYZE clinic_activity_logs;