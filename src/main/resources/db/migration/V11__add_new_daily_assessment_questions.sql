-- Migration to add new daily assessment questions and fix all UUIDs
-- This migration adds 5 new daily questions and updates all questions (daily and monthly) with real UUIDs

-- ============================================
-- STEP 1: Delete old fake UUID daily question and its options
-- ============================================

DELETE FROM assessment_questions WHERE id = '00000000-0000-0000-0000-000000000001';

-- ============================================
-- STEP 2: Add all daily assessment questions with real UUIDs
-- ============================================

-- Q1: Humor (Estado Emocional)
INSERT INTO assessment_questions (id, type, title, active)
VALUES (gen_random_uuid(), 'DAILY', 'Como está seu humor hoje?', TRUE);

-- Q3: Fissura (Intensidade) - Additional craving question
INSERT INTO assessment_questions (id, type, title, active)
VALUES (gen_random_uuid(), 'DAILY', 'Qual foi a intensidade da vontade de apostar?', TRUE);

-- Q4: Comportamento (Abstinência)
INSERT INTO assessment_questions (id, type, title, active)
VALUES (gen_random_uuid(), 'DAILY', 'Você chegou a apostar hoje?', TRUE);

-- Q6: Estresse (Gatilho Externo)
INSERT INTO assessment_questions (id, type, title, active)
VALUES (gen_random_uuid(), 'DAILY', 'Seu dia foi estressante?', TRUE);

-- Q9: Autoeficácia (Controle)
INSERT INTO assessment_questions (id, type, title, active)
VALUES (gen_random_uuid(), 'DAILY', 'Como você se sente em relação a sua recuperação?', TRUE);

-- Daily: Fissura (original)
INSERT INTO assessment_questions (id, type, title, active)
VALUES (gen_random_uuid(), 'DAILY', 'Em uma escala de 0 a 10, qual o nível de fissura/vontade de apostar hoje?', TRUE);

-- ============================================
-- STEP 3: Create options for all daily questions
-- ============================================

-- Get the UUIDs of the newly created questions for reference
DO $$
DECLARE
    original_fissura_id UUID;
    humor_question_id UUID;
    fissura_question_id UUID;
    abstinencia_question_id UUID;
    estresse_question_id UUID;
    autoeficacia_question_id UUID;
BEGIN
    -- Get question IDs by title
    SELECT id INTO original_fissura_id FROM assessment_questions WHERE title = 'Em uma escala de 0 a 10, qual o nível de fissura/vontade de apostar hoje?' AND type = 'DAILY';
    SELECT id INTO humor_question_id FROM assessment_questions WHERE title = 'Como está seu humor hoje?' AND type = 'DAILY';
    SELECT id INTO fissura_question_id FROM assessment_questions WHERE title = 'Qual foi a intensidade da vontade de apostar?' AND type = 'DAILY';
    SELECT id INTO abstinencia_question_id FROM assessment_questions WHERE title = 'Você chegou a apostar hoje?' AND type = 'DAILY';
    SELECT id INTO estresse_question_id FROM assessment_questions WHERE title = 'Seu dia foi estressante?' AND type = 'DAILY';
    SELECT id INTO autoeficacia_question_id FROM assessment_questions WHERE title = 'Como você se sente em relação a sua recuperação?' AND type = 'DAILY';

    -- Options for original Fissura question - Scale 0-10 (INVERTED: lower craving = higher score)
    INSERT INTO assessment_options (id, question_id, label, score_value, active) VALUES
        (gen_random_uuid(), original_fissura_id, '0', 10, TRUE),
        (gen_random_uuid(), original_fissura_id, '1', 9, TRUE),
        (gen_random_uuid(), original_fissura_id, '2', 8, TRUE),
        (gen_random_uuid(), original_fissura_id, '3', 7, TRUE),
        (gen_random_uuid(), original_fissura_id, '4', 6, TRUE),
        (gen_random_uuid(), original_fissura_id, '5', 5, TRUE),
        (gen_random_uuid(), original_fissura_id, '6', 4, TRUE),
        (gen_random_uuid(), original_fissura_id, '7', 3, TRUE),
        (gen_random_uuid(), original_fissura_id, '8', 2, TRUE),
        (gen_random_uuid(), original_fissura_id, '9', 1, TRUE),
        (gen_random_uuid(), original_fissura_id, '10', 0, TRUE);

    -- Options for Humor (Estado Emocional) - Scale 0-4 (INVERTED: better mood = higher score)
    INSERT INTO assessment_options (id, question_id, label, score_value, active) VALUES
        (gen_random_uuid(), humor_question_id, 'Muito bom', 4, TRUE),
        (gen_random_uuid(), humor_question_id, 'Bom', 3, TRUE),
        (gen_random_uuid(), humor_question_id, 'Neutro', 2, TRUE),
        (gen_random_uuid(), humor_question_id, 'Ruim', 1, TRUE),
        (gen_random_uuid(), humor_question_id, 'Muito ruim', 0, TRUE);

    -- Options for Fissura (Intensidade) - Scale 0-10 (INVERTED: lower craving = higher score)
    INSERT INTO assessment_options (id, question_id, label, score_value, active) VALUES
        (gen_random_uuid(), fissura_question_id, '0', 10, TRUE),
        (gen_random_uuid(), fissura_question_id, '1', 9, TRUE),
        (gen_random_uuid(), fissura_question_id, '2', 8, TRUE),
        (gen_random_uuid(), fissura_question_id, '3', 7, TRUE),
        (gen_random_uuid(), fissura_question_id, '4', 6, TRUE),
        (gen_random_uuid(), fissura_question_id, '5', 5, TRUE),
        (gen_random_uuid(), fissura_question_id, '6', 4, TRUE),
        (gen_random_uuid(), fissura_question_id, '7', 3, TRUE),
        (gen_random_uuid(), fissura_question_id, '8', 2, TRUE),
        (gen_random_uuid(), fissura_question_id, '9', 1, TRUE),
        (gen_random_uuid(), fissura_question_id, '10', 0, TRUE);

    -- Options for Comportamento (Abstinência) - Yes/No (INVERTED: no gambling = higher score)
    INSERT INTO assessment_options (id, question_id, label, score_value, active) VALUES
        (gen_random_uuid(), abstinencia_question_id, 'Não', 1, TRUE),
        (gen_random_uuid(), abstinencia_question_id, 'Sim', 0, TRUE);

    -- Options for Estresse (Gatilho Externo) - Scale 0-4 (INVERTED: less stress = higher score)
    INSERT INTO assessment_options (id, question_id, label, score_value, active) VALUES
        (gen_random_uuid(), estresse_question_id, 'Nada estressante', 4, TRUE),
        (gen_random_uuid(), estresse_question_id, 'Pouco estressante', 3, TRUE),
        (gen_random_uuid(), estresse_question_id, 'Moderadamente estressante', 2, TRUE),
        (gen_random_uuid(), estresse_question_id, 'Muito estressante', 1, TRUE),
        (gen_random_uuid(), estresse_question_id, 'Extremamente estressante', 0, TRUE);

    -- Options for Autoeficácia (Controle) - Scale 0-4 (INVERTED: more confident = higher score)
    INSERT INTO assessment_options (id, question_id, label, score_value, active) VALUES
        (gen_random_uuid(), autoeficacia_question_id, 'Muito confiante', 4, TRUE),
        (gen_random_uuid(), autoeficacia_question_id, 'Confiante', 3, TRUE),
        (gen_random_uuid(), autoeficacia_question_id, 'Neutro', 2, TRUE),
        (gen_random_uuid(), autoeficacia_question_id, 'Inseguro', 1, TRUE),
        (gen_random_uuid(), autoeficacia_question_id, 'Muito inseguro', 0, TRUE);

END $$;

-- ============================================
-- STEP 3: Update existing monthly (PGSI) questions with real UUIDs
-- ============================================

-- Delete old fake UUID questions and their options (cascade will handle options)
-- Then re-insert with real UUIDs

-- First, delete existing fake monthly questions
DELETE FROM assessment_questions WHERE id IN (
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000102',
    '00000000-0000-0000-0000-000000000103',
    '00000000-0000-0000-0000-000000000104',
    '00000000-0000-0000-0000-000000000105',
    '00000000-0000-0000-0000-000000000106',
    '00000000-0000-0000-0000-000000000107',
    '00000000-0000-0000-0000-000000000108',
    '00000000-0000-0000-0000-000000000109'
);

-- Insert monthly questions with real UUIDs
INSERT INTO assessment_questions (id, type, title, active) VALUES
    (gen_random_uuid(), 'MONTHLY', 'Nos últimos 12 meses, com que frequência você apostou mais do que podia perder?', TRUE),
    (gen_random_uuid(), 'MONTHLY', 'Nos últimos 12 meses, você precisou apostar quantias maiores para obter a mesma sensação?', TRUE),
    (gen_random_uuid(), 'MONTHLY', 'Nos últimos 12 meses, você voltou para recuperar o dinheiro perdido ao apostar?', TRUE),
    (gen_random_uuid(), 'MONTHLY', 'Nos últimos 12 meses, você pegou dinheiro emprestado ou vendeu algo para apostar?', TRUE),
    (gen_random_uuid(), 'MONTHLY', 'Nos últimos 12 meses, você sentiu que poderia ter um problema com apostas?', TRUE),
    (gen_random_uuid(), 'MONTHLY', 'Nos últimos 12 meses, pessoas criticaram suas apostas ou disseram que você tinha um problema?', TRUE),
    (gen_random_uuid(), 'MONTHLY', 'Nos últimos 12 meses, suas apostas causaram problemas financeiros para você ou sua família?', TRUE),
    (gen_random_uuid(), 'MONTHLY', 'Nos últimos 12 meses, você se sentiu culpado por apostar?', TRUE),
    (gen_random_uuid(), 'MONTHLY', 'Nos últimos 12 meses, você teve problemas de saúde (incluindo estresse ou ansiedade) por causa das apostas?', TRUE);

-- ============================================
-- STEP 4: Create options for monthly questions
-- ============================================

DO $$
DECLARE
    q1_id UUID;
    q2_id UUID;
    q3_id UUID;
    q4_id UUID;
    q5_id UUID;
    q6_id UUID;
    q7_id UUID;
    q8_id UUID;
    q9_id UUID;
BEGIN
    -- Get monthly question IDs by title
    SELECT id INTO q1_id FROM assessment_questions WHERE title = 'Nos últimos 12 meses, com que frequência você apostou mais do que podia perder?' AND type = 'MONTHLY';
    SELECT id INTO q2_id FROM assessment_questions WHERE title = 'Nos últimos 12 meses, você precisou apostar quantias maiores para obter a mesma sensação?' AND type = 'MONTHLY';
    SELECT id INTO q3_id FROM assessment_questions WHERE title = 'Nos últimos 12 meses, você voltou para recuperar o dinheiro perdido ao apostar?' AND type = 'MONTHLY';
    SELECT id INTO q4_id FROM assessment_questions WHERE title = 'Nos últimos 12 meses, você pegou dinheiro emprestado ou vendeu algo para apostar?' AND type = 'MONTHLY';
    SELECT id INTO q5_id FROM assessment_questions WHERE title = 'Nos últimos 12 meses, você sentiu que poderia ter um problema com apostas?' AND type = 'MONTHLY';
    SELECT id INTO q6_id FROM assessment_questions WHERE title = 'Nos últimos 12 meses, pessoas criticaram suas apostas ou disseram que você tinha um problema?' AND type = 'MONTHLY';
    SELECT id INTO q7_id FROM assessment_questions WHERE title = 'Nos últimos 12 meses, suas apostas causaram problemas financeiros para você ou sua família?' AND type = 'MONTHLY';
    SELECT id INTO q8_id FROM assessment_questions WHERE title = 'Nos últimos 12 meses, você se sentiu culpado por apostar?' AND type = 'MONTHLY';
    SELECT id INTO q9_id FROM assessment_questions WHERE title = 'Nos últimos 12 meses, você teve problemas de saúde (incluindo estresse ou ansiedade) por causa das apostas?' AND type = 'MONTHLY';

    -- Options for all monthly questions (same options: Nunca, Às vezes, A maioria das vezes, Quase sempre)
    INSERT INTO assessment_options (id, question_id, label, score_value, active) VALUES
        -- Q1
        (gen_random_uuid(), q1_id, 'Nunca', 0, TRUE),
        (gen_random_uuid(), q1_id, 'Às vezes', 1, TRUE),
        (gen_random_uuid(), q1_id, 'A maioria das vezes', 2, TRUE),
        (gen_random_uuid(), q1_id, 'Quase sempre', 3, TRUE),
        -- Q2
        (gen_random_uuid(), q2_id, 'Nunca', 0, TRUE),
        (gen_random_uuid(), q2_id, 'Às vezes', 1, TRUE),
        (gen_random_uuid(), q2_id, 'A maioria das vezes', 2, TRUE),
        (gen_random_uuid(), q2_id, 'Quase sempre', 3, TRUE),
        -- Q3
        (gen_random_uuid(), q3_id, 'Nunca', 0, TRUE),
        (gen_random_uuid(), q3_id, 'Às vezes', 1, TRUE),
        (gen_random_uuid(), q3_id, 'A maioria das vezes', 2, TRUE),
        (gen_random_uuid(), q3_id, 'Quase sempre', 3, TRUE),
        -- Q4
        (gen_random_uuid(), q4_id, 'Nunca', 0, TRUE),
        (gen_random_uuid(), q4_id, 'Às vezes', 1, TRUE),
        (gen_random_uuid(), q4_id, 'A maioria das vezes', 2, TRUE),
        (gen_random_uuid(), q4_id, 'Quase sempre', 3, TRUE),
        -- Q5
        (gen_random_uuid(), q5_id, 'Nunca', 0, TRUE),
        (gen_random_uuid(), q5_id, 'Às vezes', 1, TRUE),
        (gen_random_uuid(), q5_id, 'A maioria das vezes', 2, TRUE),
        (gen_random_uuid(), q5_id, 'Quase sempre', 3, TRUE),
        -- Q6
        (gen_random_uuid(), q6_id, 'Nunca', 0, TRUE),
        (gen_random_uuid(), q6_id, 'Às vezes', 1, TRUE),
        (gen_random_uuid(), q6_id, 'A maioria das vezes', 2, TRUE),
        (gen_random_uuid(), q6_id, 'Quase sempre', 3, TRUE),
        -- Q7
        (gen_random_uuid(), q7_id, 'Nunca', 0, TRUE),
        (gen_random_uuid(), q7_id, 'Às vezes', 1, TRUE),
        (gen_random_uuid(), q7_id, 'A maioria das vezes', 2, TRUE),
        (gen_random_uuid(), q7_id, 'Quase sempre', 3, TRUE),
        -- Q8
        (gen_random_uuid(), q8_id, 'Nunca', 0, TRUE),
        (gen_random_uuid(), q8_id, 'Às vezes', 1, TRUE),
        (gen_random_uuid(), q8_id, 'A maioria das vezes', 2, TRUE),
        (gen_random_uuid(), q8_id, 'Quase sempre', 3, TRUE),
        -- Q9
        (gen_random_uuid(), q9_id, 'Nunca', 0, TRUE),
        (gen_random_uuid(), q9_id, 'Às vezes', 1, TRUE),
        (gen_random_uuid(), q9_id, 'A maioria das vezes', 2, TRUE),
        (gen_random_uuid(), q9_id, 'Quase sempre', 3, TRUE);

END $$;
