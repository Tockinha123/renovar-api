package com.tocka.renovarAPI.assessment.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tocka.renovarAPI.bets.Bet;
import com.tocka.renovarAPI.bets.BetRepository;
import com.tocka.renovarAPI.metrics.PatientMetrics;
import com.tocka.renovarAPI.metrics.RiskLevel;
import com.tocka.renovarAPI.patient.Patient;

@Service
public class AssessmentScoringService {

    private final BetRepository betRepository;
    private final Clock clock;

    @Autowired
    public AssessmentScoringService(BetRepository betRepository) {
        this(betRepository, Clock.systemDefaultZone());
    }

    AssessmentScoringService(BetRepository betRepository, Clock clock) {
        this.betRepository = betRepository;
        this.clock = clock;
    }

    public int calculateP4(int craving) {
        return 120 - (craving * 12);
    }

    public RiskLevel calculatePgsiRiskLevel(int pgsiScore) {
        if (pgsiScore == 0) {
            return RiskLevel.EXCELENTE;
        }
        if (pgsiScore <= 2) {
            return RiskLevel.BOM;
        }
        if (pgsiScore <= 7) {
            return RiskLevel.REGULAR;
        }
        return RiskLevel.ALTO_RISCO;
    }

    public double calculateVariation(Integer previousScore, Integer currentScore) {
        if (previousScore == null || previousScore == 0) {
            return 0.0;
        }
        return ((double) currentScore - previousScore);
    }

    public RiskLevel deriveRiskLevelFromScore(int score) {
        if (score >= 701) {
            return RiskLevel.EXCELENTE;
        }
        if (score >= 501) {
            return RiskLevel.BOM;
        }
        if (score >= 301) {
            return RiskLevel.REGULAR;
        }
        return RiskLevel.ALTO_RISCO;
    }

    public ScoreCalculationResult recalculateFullScore(Patient patient, PatientMetrics metrics, int todayCraving) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime start = today.minusDays(29).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<Bet> betsLast30Days = betRepository.findByPatientAndCreatedAtBetween(patient, start, end);
        Set<LocalDate> daysWithBet = betsLast30Days.stream()
                .map(bet -> bet.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());
        int cleanDays = Math.max(0, 30 - daysWithBet.size());
        int p1Score = Math.round((cleanDays / 30.0f) * 290);

        int streak = metrics.getCleanDaysStreak() == null ? 0 : metrics.getCleanDaysStreak();
        int p2Score = Math.min(240, Math.round((streak / 30.0f) * 240));

        Bet lastBet = betRepository.findTopByPatientOrderByCreatedAtDesc(patient);
        int p3Score;
        if (lastBet == null) {
            p3Score = 210;
        } else {
            BigDecimal baseline = patient.getFinancialBaseline() != null ? patient.getFinancialBaseline() : BigDecimal.ZERO;
            p3Score = lastBet.getAmount().compareTo(baseline) > 0 ? 0 : 150;
        }

        int p4Score = calculateP4(todayCraving);
        int p5Score = 80;
        int p6Score = 0;

        return ScoreCalculationResult.fromPillars(p1Score, p2Score, p3Score, p4Score, p5Score, p6Score);
    }
}
