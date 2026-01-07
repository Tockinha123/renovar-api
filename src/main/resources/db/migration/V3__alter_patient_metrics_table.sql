-- Adiciona coluna de nível de risco atual
ALTER TABLE patient_metrics 
ADD COLUMN current_risk_level VARCHAR(20) NOT NULL DEFAULT 'BOM';

-- Adiciona coluna de última aposta
ALTER TABLE patient_metrics 
ADD COLUMN last_bet_at TIMESTAMP;

-- Adiciona coluna de tempo recuperado
ALTER TABLE patient_metrics 
ADD COLUMN time_recovered INTEGER NOT NULL DEFAULT 0;

-- Constraint para validar risk level
ALTER TABLE patient_metrics 
ADD CONSTRAINT chk_risk_level CHECK (current_risk_level IN ('EXCELENTE', 'BOM', 'REGULAR', 'ALTO_RISCO'));

-- Atualiza registros existentes baseado no score atual
UPDATE patient_metrics 
SET current_risk_level = CASE 
    WHEN current_score >= 701 THEN 'EXCELENTE'
    WHEN current_score >= 501 THEN 'BOM'
    WHEN current_score >= 301 THEN 'REGULAR'
    ELSE 'ALTO_RISCO'
END;