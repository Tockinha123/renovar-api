CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    reference_month INTEGER NOT NULL,
    reference_year INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_reports_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT chk_report_status CHECK (status IN ('GENERATING', 'COMPLETED', 'ERROR')),
    CONSTRAINT chk_reference_month CHECK (reference_month BETWEEN 1 AND 12),
    CONSTRAINT uq_patient_month_year UNIQUE (patient_id, reference_month, reference_year)
);

CREATE INDEX idx_reports_patient_id ON reports(patient_id);
CREATE INDEX idx_reports_reference ON reports(reference_year, reference_month);