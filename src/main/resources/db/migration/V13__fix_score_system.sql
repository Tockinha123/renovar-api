-- =====================================================================
-- V13__fix_score_system_v2.sql
-- =====================================================================
INSERT INTO score_history (
    id, patient_id, total_score,
    p1_score, p2_score, p3_score, p4_score, p5_score, p6_score,
    score_risk_level, pgsi_risk_level, recorded_at,
    calculation_source, recalculated_pillars
)
SELECT
    gen_random_uuid(),
    p.id,
    500,
    290, 0, 210, 0, 0, 0,
    'BOM',
    NULL,
    p.created_at,
    'MANUAL_RECALCULATION',
    'p1,p2,p3,p4,p5,p6'
FROM patients p
WHERE NOT EXISTS (
    SELECT 1 FROM score_history sh WHERE sh.patient_id = p.id
);