-- Fix Portuguese accents in assessment questions and options
-- This migration updates existing ASCII text with proper UTF-8 accented characters

-- Daily craving question
UPDATE assessment_questions
SET title = 'Em uma escala de 0 a 10, qual o nível de fissura/vontade de apostar hoje?'
WHERE id = '00000000-0000-0000-0000-000000000001'
  AND title = 'Em uma escala de 0 a 10, qual o nivel de fissura/vontade de apostar hoje?';

-- PGSI questions (monthly)
UPDATE assessment_questions
SET title = 'Nos últimos 12 meses, com que frequência você apostou mais do que podia perder?'
WHERE id = '00000000-0000-0000-0000-000000000101'
  AND title = 'Nos ultimos 12 meses, com que frequencia voce apostou mais do que podia perder?';

UPDATE assessment_questions
SET title = 'Nos últimos 12 meses, você precisou apostar quantias maiores para obter a mesma sensação?'
WHERE id = '00000000-0000-0000-0000-000000000102'
  AND title = 'Nos ultimos 12 meses, voce precisou apostar quantias maiores para obter a mesma sensacao?';

UPDATE assessment_questions
SET title = 'Nos últimos 12 meses, você voltou para recuperar o dinheiro perdido ao apostar?'
WHERE id = '00000000-0000-0000-0000-000000000103'
  AND title = 'Nos ultimos 12 meses, voce voltou para recuperar o dinheiro perdido ao apostar?';

UPDATE assessment_questions
SET title = 'Nos últimos 12 meses, você pegou dinheiro emprestado ou vendeu algo para apostar?'
WHERE id = '00000000-0000-0000-0000-000000000104'
  AND title = 'Nos ultimos 12 meses, voce pegou dinheiro emprestado ou vendeu algo para apostar?';

UPDATE assessment_questions
SET title = 'Nos últimos 12 meses, você sentiu que poderia ter um problema com apostas?'
WHERE id = '00000000-0000-0000-0000-000000000105'
  AND title = 'Nos ultimos 12 meses, voce sentiu que poderia ter um problema com apostas?';

UPDATE assessment_questions
SET title = 'Nos últimos 12 meses, pessoas criticaram suas apostas ou disseram que você tinha um problema?'
WHERE id = '00000000-0000-0000-0000-000000000106'
  AND title = 'Nos ultimos 12 meses, pessoas criticaram suas apostas ou disseram que voce tinha um problema?';

UPDATE assessment_questions
SET title = 'Nos últimos 12 meses, suas apostas causaram problemas financeiros para você ou sua família?'
WHERE id = '00000000-0000-0000-0000-000000000107'
  AND title = 'Nos ultimos 12 meses, suas apostas causaram problemas financeiros para voce ou sua familia?';

UPDATE assessment_questions
SET title = 'Nos últimos 12 meses, você se sentiu culpado por apostar?'
WHERE id = '00000000-0000-0000-0000-000000000108'
  AND title = 'Nos ultimos 12 meses, voce se sentiu culpado por apostar?';

UPDATE assessment_questions
SET title = 'Nos últimos 12 meses, você teve problemas de saúde (incluindo estresse ou ansiedade) por causa das apostas?'
WHERE id = '00000000-0000-0000-0000-000000000109'
  AND title = 'Nos ultimos 12 meses, voce teve problemas de saude (incluindo estresse ou ansiedade) por causa das apostas?';

-- PGSI options: "As vezes" -> "Às vezes" for all questions
UPDATE assessment_options SET label = 'Às vezes' WHERE label = 'As vezes';
