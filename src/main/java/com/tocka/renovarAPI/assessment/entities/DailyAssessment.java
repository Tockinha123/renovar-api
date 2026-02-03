package com.tocka.renovarAPI.assessment.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    name = "daily_assessments",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_daily_assessment_patient_date",
        columnNames = {"patient_id", "assessment_date"}
    )
)
@Data
public class DailyAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "craving_level", nullable = false)
    private Integer cravingLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.assessmentDate == null) {
            this.assessmentDate = LocalDate.now();
        }
    }
}
