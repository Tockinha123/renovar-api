package com.tocka.renovarAPI.assessment.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "assessment_answers")
@Data
public class AssessmentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "daily_assessment_id")
    private DailyAssessment dailyAssessment;

    @ManyToOne
    @JoinColumn(name = "monthly_assessment_id")
    private MonthlyAssessment monthlyAssessment;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @ManyToOne
    @JoinColumn(name = "option_id", nullable = false)
    private AssessmentOption option;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        validateXor();
    }

    private void validateXor() {
        boolean bothNull = dailyAssessment == null && monthlyAssessment == null;
        boolean bothNotNull = dailyAssessment != null && monthlyAssessment != null;

        if (bothNull || bothNotNull) {
            throw new IllegalStateException(
                "Exactly one of dailyAssessment or monthlyAssessment must be set"
            );
        }
    }
}
