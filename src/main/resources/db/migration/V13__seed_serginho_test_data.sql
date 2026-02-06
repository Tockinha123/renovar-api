-- =====================================================
-- Dados de TESTE para o paciente serginho@email.com
-- Simula uso desde Dezembro/2025 para testar relatórios
-- =====================================================

-- 1. Altera created_at do user e patient para simular conta antiga (10/12/2025)
UPDATE users 
SET created_at = '2025-12-10 10:00:00' 
WHERE id = 'eeafad50-a3de-4388-b758-e15cb6e29a48';

UPDATE patients 
SET created_at = '2025-12-10 10:00:00' 
WHERE id = '1b901c03-18e1-4fb0-8ba8-5d406abce372';

-- =====================================================
-- 2. Apostas em DEZEMBRO/2025 (algumas recaídas)
-- =====================================================
INSERT INTO bets (patient_id, amount, won, session_time, category, created_at) VALUES
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 30.00, false, 'THIRTY_MINUTES', 'CASSINO', '2025-12-12 20:00:00'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 50.00, true,  'SIXTY_PLUS_MINUTES', 'ESPORTES', '2025-12-15 21:30:00'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 20.00, false, 'FIFTEEN_MINUTES', 'CASSINO', '2025-12-18 19:00:00'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 100.00, false, 'SIXTY_PLUS_MINUTES', 'ESPORTES', '2025-12-22 22:00:00'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 15.00, false, 'FIVE_MINUTES', 'CASSINO', '2025-12-27 23:00:00');

-- =====================================================
-- 3. Apostas em JANEIRO/2026 (melhorando)
-- =====================================================
INSERT INTO bets (patient_id, amount, won, session_time, category, created_at) VALUES
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 40.00, false, 'THIRTY_MINUTES', 'ESPORTES', '2026-01-05 18:00:00'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 25.00, true,  'FIFTEEN_MINUTES', 'CASSINO', '2026-01-12 20:00:00'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 60.00, false, 'SIXTY_PLUS_MINUTES', 'ESPORTES', '2026-01-20 14:30:00');

-- =====================================================
-- 4. Score History - Dezembro/2025
-- =====================================================
INSERT INTO score_history (patient_id, total_score, p1_score, p2_score, p3_score, p4_score, p5_score, p6_score, score_risk_level, recorded_at, calculation_source, recalculated_pillars) VALUES
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 500, 80, 85, 90, 75, 80, 90, 'BOM', '2025-12-10 10:00:00', 'MANUAL_RECALCULATION', 'p1,p2,p3,p4,p5,p6'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 480, 75, 80, 85, 70, 80, 90, 'REGULAR', '2025-12-15 21:30:00', 'MANUAL_RECALCULATION', 'p1,p2,p3,p4,p5,p6'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 520, 82, 85, 88, 78, 82, 105, 'BOM', '2025-12-25 08:00:00', 'MANUAL_RECALCULATION', 'p1,p2,p3,p4,p5,p6');

-- =====================================================
-- 5. Score History - Janeiro/2026
-- =====================================================
INSERT INTO score_history (patient_id, total_score, p1_score, p2_score, p3_score, p4_score, p5_score, p6_score, score_risk_level, recorded_at, calculation_source, recalculated_pillars) VALUES
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 550, 85, 88, 90, 80, 95, 112, 'BOM', '2026-01-05 08:00:00', 'MANUAL_RECALCULATION', 'p1,p2,p3,p4,p5,p6'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 580, 88, 90, 92, 82, 98, 130, 'BOM', '2026-01-15 08:00:00', 'MANUAL_RECALCULATION', 'p1,p2,p3,p4,p5,p6'),
('1b901c03-18e1-4fb0-8ba8-5d406abce372', 620, 92, 95, 95, 85, 100, 153, 'BOM', '2026-01-28 08:00:00', 'MANUAL_RECALCULATION', 'p1,p2,p3,p4,p5,p6');
