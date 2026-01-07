package com.tocka.renovarAPI.metrics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.tocka.renovarAPI.patient.Patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "patient_metrics")
@Data
public class PatientMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "patient_id", nullable = false, unique = true)
    private Patient patient;

    @Column(name = "current_score", nullable = false)
    private Integer currentScore = 500;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_risk_level", nullable = false)
    private RiskLevel currentRiskLevel = RiskLevel.BOM; // 500 pts = BOM

    @Column(name = "clean_days_streak", nullable = false)
    private Integer cleanDaysStreak = 0;

    @Column(name = "savings_accumulated", nullable = false, precision = 10, scale = 2)
    private BigDecimal savingsAccumulated = BigDecimal.ZERO;

    @Column(name = "time_recovered", nullable = false)
    private Integer timeRecovered = 0; // em minutos

    @Column(name = "last_checkin")
    private LocalDateTime lastCheckin;

    @Column(name = "last_bet_at")
    private LocalDateTime lastBetAt;
}