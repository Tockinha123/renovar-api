ALTER TABLE assessment_answers 
ADD CONSTRAINT chk_assessment_xor 
CHECK (
    (daily_assessment_id IS NULL AND monthly_assessment_id IS NOT NULL) OR
    (daily_assessment_id IS NOT NULL AND monthly_assessment_id IS NULL)
);
