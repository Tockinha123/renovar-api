package com.tocka.renovarAPI.score;

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
import com.tocka.renovarAPI.score.model.AssessmentPillarScores;
import com.tocka.renovarAPI.score.model.BetPillarScores;

/**
 * Service responsible for calculating pillar scores.
 * Separates bet-related pillars (p1-p3) from assessment-related pillars (p4-p6).
 */
@Service
public class ScoreCalculationService {

    private final BetRepository betRepository;
    private final Clock clock;

    @Autowired
    public ScoreCalculationService(BetRepository betRepository) {
        this(betRepository, Clock.systemDefaultZone());
    }

    ScoreCalculationService(BetRepository betRepository, Clock clock) {
        this.betRepository = betRepository;
        this.clock = clock;
    }

    /**
     * Calculate bet-related pillars (p1, p2, p3).
     * 
     * P1: Clean days ratio - based on days without bet in last 30 days (max 290)
     * P2: Streak - based on current clean days streak (max 240)
     * P3: Financial - based on last bet amount vs baseline (0, 150, or 210)
     * 
     * @param patient The patient
     * @param metrics The patient's metrics containing streak info
     * @return BetPillarScores with p1, p2, p3 values
     */
    public BetPillarScores calculateBetPillars(Patient patient, PatientMetrics metrics) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime start = today.minusDays(29).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);

        // P1: Clean days ratio
        List<Bet> betsLast30Days = betRepository.findByPatientAndCreatedAtBetween(patient, start, end);
        Set<LocalDate> daysWithBet = betsLast30Days.stream()
                .map(bet -> bet.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());
        int cleanDays = Math.max(0, 30 - daysWithBet.size());
        int p1Score = Math.round((cleanDays / 30.0f) * 290);

        // P2: Streak
        int streak = metrics.getCleanDaysStreak() == null ? 0 : metrics.getCleanDaysStreak();
        int p2Score = Math.min(240, Math.round((streak / 30.0f) * 240));

        // P3: Financial
        Bet lastBet = betRepository.findTopByPatientOrderByCreatedAtDesc(patient);
        int p3Score;
        if (lastBet == null) {
            p3Score = 210;
        } else {
            BigDecimal baseline = patient.getFinancialBaseline() != null ? patient.getFinancialBaseline() : BigDecimal.ZERO;
            p3Score = lastBet.getAmount().compareTo(baseline) > 0 ? 0 : 150;
        }

        return new BetPillarScores(p1Score, p2Score, p3Score);
    }

    /**
     * Calculate assessment-related pillars (p4, p5, p6).
     * 
     * P4: Craving control - based on daily craving level (0-10), higher craving = lower score
     * P5: Fixed score for completing daily check-in (80 points)
     * P6: Reserved for future use (0 points)
     * 
     * @param todayCraving The craving level from daily assessment (0-10)
     * @return AssessmentPillarScores with p4, p5, p6 values
     */
    public AssessmentPillarScores calculateAssessmentPillars(int todayCraving) {
        int p4Score = calculateP4(todayCraving);
        int p5Score = 80;
        int p6Score = 0;

        return new AssessmentPillarScores(p4Score, p5Score, p6Score);
    }

    /**
     * Calculate P4 score from craving level.
     * Formula: 120 - (craving * 12)
     * Range: 0 (craving=10) to 120 (craving=0)
     */
    public int calculateP4(int craving) {
        return 120 - (craving * 12);
    }

    /**
     * Calculate total score from all pillars.
     */
    public int calculateTotalScore(BetPillarScores betScores, AssessmentPillarScores assessmentScores) {
        return betScores.total() + assessmentScores.total();
    }

    /**
     * Calculate total score from individual pillar values.
     */
    public int calculateTotalScore(int p1, int p2, int p3, int p4, int p5, int p6) {
        return p1 + p2 + p3 + p4 + p5 + p6;
    }

    public RiskLevel deriveRiskLevelFromScore(int score) {
        return RiskLevel.fromScore(score);
    }

    public RiskLevel calculatePgsiRiskLevel(int pgsiScore) {
        return RiskLevel.fromPgsi(pgsiScore);
    }

    /**
     * Calculate score variation between previous and current scores.
     */
    public double calculateVariation(Integer previousScore, Integer currentScore) {
        if (previousScore == null || previousScore == 0) {
            return 0.0;
        }
        return ((double) currentScore - previousScore);
    }
}
