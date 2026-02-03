package com.tocka.renovarAPI.assessment.entities;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

import com.tocka.renovarAPI.patient.Patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(
    name = "monthly_assessments",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_monthly_assessment_patient_month_year",
        columnNames = {"patient_id", "reference_month", "reference_year"}
    )
)
@Data
public class MonthlyAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "reference_month", nullable = false)
    private Integer referenceMonth;

    @Column(name = "reference_year", nullable = false)
    private Integer referenceYear;

    @Column(name = "pgsi_score", nullable = false)
    private Integer pgsiScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.referenceMonth == null || this.referenceYear == null) {
            YearMonth now = YearMonth.now();
            if (this.referenceMonth == null) {
                this.referenceMonth = now.getMonthValue();
            }
            if (this.referenceYear == null) {
                this.referenceYear = now.getYear();
            }
        }
    }
}
