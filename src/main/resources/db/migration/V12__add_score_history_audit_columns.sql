-- Add audit columns to score_history table for tracking calculation source and affected pillars

-- Add new columns
ALTER TABLE score_history
    ADD COLUMN pgsi_score INTEGER,
    ADD COLUMN calculation_source VARCHAR(50) NOT NULL DEFAULT 'MANUAL_RECALCULATION',
    ADD COLUMN trigger_entity_id UUID,
    ADD COLUMN recalculated_pillars VARCHAR(50) NOT NULL DEFAULT 'p1,p2,p3,p4,p5,p6';

-- Create index for efficient querying by patient and recorded_at
CREATE INDEX idx_score_history_patient_recorded ON score_history(patient_id, recorded_at DESC);
