-- Add indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_timestamp_desc 
    ON public.clinic_activity_logs (event_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_clinic_activity_logs_composite 
    ON public.clinic_activity_logs (event_timestamp DESC, activity_type, status_flag);

-- Add table partitioning
CREATE TABLE IF NOT EXISTS clinic_activity_logs_partitioned (
    LIKE clinic_activity_logs INCLUDING ALL
) PARTITION BY RANGE (event_timestamp);

-- Create partitions for last 3 months
CREATE TABLE IF NOT EXISTS clinic_activity_logs_y2025m05 
    PARTITION OF clinic_activity_logs_partitioned 
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');

CREATE TABLE IF NOT EXISTS clinic_activity_logs_y2025m06 
    PARTITION OF clinic_activity_logs_partitioned 
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

CREATE TABLE IF NOT EXISTS clinic_activity_logs_y2025m07 
    PARTITION OF clinic_activity_logs_partitioned 
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');