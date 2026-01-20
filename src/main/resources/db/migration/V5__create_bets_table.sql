-- Tabela de registros de apostas
CREATE TABLE bets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    won BOOLEAN NOT NULL,
    session_time VARCHAR(30) NOT NULL,
    category VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_bets_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT chk_session_time CHECK (session_time IN ('FIVE_MINUTES', 'FIFTEEN_MINUTES', 'THIRTY_MINUTES', 'SIXTY_PLUS_MINUTES')),
    CONSTRAINT chk_category CHECK (category IN ('CASSINO', 'ESPORTES', 'CARTAS', 'OUTROS'))
);

-- Índices para performance
CREATE INDEX idx_bets_patient_id ON bets(patient_id);
CREATE INDEX idx_bets_created_at ON bets(created_at);