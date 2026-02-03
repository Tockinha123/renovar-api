-- Tabelas para avaliações diárias e mensais
CREATE TABLE assessment_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(20) NOT NULL,
    title VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_assessment_question_type CHECK (type IN ('DAILY', 'MONTHLY'))
);

CREATE TABLE assessment_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL,
    label VARCHAR(255) NOT NULL,
    score_value INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_assessment_options_question FOREIGN KEY (question_id) REFERENCES assessment_questions(id) ON DELETE CASCADE
);

CREATE TABLE daily_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    assessment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    craving_level INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_daily_assessments_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT uq_daily_assessment_patient_date UNIQUE (patient_id, assessment_date),
    CONSTRAINT chk_daily_assessment_craving CHECK (craving_level BETWEEN 0 AND 10)
);

CREATE TABLE monthly_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    reference_month INTEGER NOT NULL,
    reference_year INTEGER NOT NULL,
    pgsi_score INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_monthly_assessments_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT uq_monthly_assessment_patient_month_year UNIQUE (patient_id, reference_month, reference_year),
    CONSTRAINT chk_monthly_assessment_month CHECK (reference_month BETWEEN 1 AND 12),
    CONSTRAINT chk_monthly_assessment_pgsi CHECK (pgsi_score BETWEEN 0 AND 27)
);

CREATE TABLE assessment_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_assessment_id UUID,
    monthly_assessment_id UUID,
    question_id UUID NOT NULL,
    option_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_assessment_answers_daily FOREIGN KEY (daily_assessment_id) REFERENCES daily_assessments(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_answers_monthly FOREIGN KEY (monthly_assessment_id) REFERENCES monthly_assessments(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_answers_question FOREIGN KEY (question_id) REFERENCES assessment_questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_answers_option FOREIGN KEY (option_id) REFERENCES assessment_options(id) ON DELETE CASCADE,
    CONSTRAINT chk_assessment_answer_parent CHECK (
        (daily_assessment_id IS NOT NULL AND monthly_assessment_id IS NULL)
        OR (daily_assessment_id IS NULL AND monthly_assessment_id IS NOT NULL)
    )
);

CREATE TABLE score_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    total_score INTEGER NOT NULL,
    p1_score INTEGER NOT NULL,
    p2_score INTEGER NOT NULL,
    p3_score INTEGER NOT NULL,
    p4_score INTEGER NOT NULL,
    p5_score INTEGER NOT NULL,
    p6_score INTEGER NOT NULL,
    score_risk_level VARCHAR(20) NOT NULL,
    pgsi_risk_level VARCHAR(20),
    recorded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_score_history_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT chk_score_history_score_risk CHECK (score_risk_level IN ('EXCELENTE', 'BOM', 'REGULAR', 'ALTO_RISCO')),
    CONSTRAINT chk_score_history_pgsi_risk CHECK (
        pgsi_risk_level IS NULL OR pgsi_risk_level IN ('EXCELENTE', 'BOM', 'REGULAR', 'ALTO_RISCO')
    )
);

CREATE INDEX idx_assessment_questions_type ON assessment_questions(type);
CREATE INDEX idx_assessment_options_question_id ON assessment_options(question_id);
CREATE INDEX idx_daily_assessments_patient_id ON daily_assessments(patient_id);
CREATE INDEX idx_monthly_assessments_patient_id ON monthly_assessments(patient_id);
CREATE INDEX idx_assessment_answers_question_id ON assessment_answers(question_id);
CREATE INDEX idx_score_history_patient_id ON score_history(patient_id);
CREATE INDEX idx_score_history_recorded_at ON score_history(recorded_at);
