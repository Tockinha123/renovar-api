package com.tocka.renovarAPI.score.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.tocka.renovarAPI.metrics.RiskLevel;
import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.score.model.CalculationSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "score_history")
@Data
public class ScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "p1_score", nullable = false)
    private Integer p1Score;

    @Column(name = "p2_score", nullable = false)
    private Integer p2Score;

    @Column(name = "p3_score", nullable = false)
    private Integer p3Score;

    @Column(name = "p4_score", nullable = false)
    private Integer p4Score;

    @Column(name = "p5_score", nullable = false)
    private Integer p5Score;

    @Column(name = "p6_score", nullable = false)
    private Integer p6Score;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_risk_level", nullable = false)
    private RiskLevel scoreRiskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "pgsi_risk_level")
    private RiskLevel pgsiRiskLevel;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    // New audit fields
    @Column(name = "pgsi_score")
    private Integer pgsiScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_source", nullable = false)
    private CalculationSource calculationSource;

    @Column(name = "trigger_entity_id")
    private UUID triggerEntityId;

    @Column(name = "recalculated_pillars", nullable = false)
    private String recalculatedPillars;

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }
}
