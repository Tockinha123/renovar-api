-- Tabela de pacientes (dados do onboarding)
CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    financial_baseline DECIMAL(10, 2) NOT NULL,
    session_time_baseline INTEGER NOT NULL,
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Tabela de métricas do paciente 
CREATE TABLE patient_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL UNIQUE,
    current_score INTEGER NOT NULL DEFAULT 500,
    clean_days_streak INTEGER NOT NULL DEFAULT 0,
    savings_accumulated DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    last_checkin TIMESTAMP,
    CONSTRAINT fk_patient_metrics_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);


CREATE INDEX idx_patients_user_id ON patients(user_id);
CREATE INDEX idx_patient_metrics_patient_id ON patient_metrics(patient_id);