package com.tocka.renovarAPI.score;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tocka.renovarAPI.metrics.RiskLevel;
import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.score.entity.ScoreHistory;
import com.tocka.renovarAPI.score.model.AssessmentPillarScores;
import com.tocka.renovarAPI.score.model.BetPillarScores;
import com.tocka.renovarAPI.score.model.CalculationSource;
import com.tocka.renovarAPI.score.repository.ScoreHistoryRepository;

/**
 * Service responsible for managing score history records.
 * Provides methods to create, query, and manage score history entries.
 */
@Service
public class ScoreHistoryService {

    private final ScoreHistoryRepository scoreHistoryRepository;
    private final ScoreCalculationService scoreCalculationService;

    public ScoreHistoryService(ScoreHistoryRepository scoreHistoryRepository, 
                               ScoreCalculationService scoreCalculationService) {
        this.scoreHistoryRepository = scoreHistoryRepository;
        this.scoreCalculationService = scoreCalculationService;
    }

    /**
     * Create a new score history entry.
     * 
     * @param patient The patient
     * @param p1Score Bet pillar 1 (clean days ratio)
     * @param p2Score Bet pillar 2 (streak)
     * @param p3Score Bet pillar 3 (financial)
     * @param p4Score Assessment pillar 4 (craving)
     * @param p5Score Assessment pillar 5 (check-in)
     * @param p6Score Assessment pillar 6 (reserved)
     * @param pgsiRiskLevel PGSI risk level (nullable)
     * @param pgsiScore Raw PGSI score (nullable, only for monthly assessments)
     * @param calculationSource What triggered this score calculation
     * @param triggerEntityId ID of the entity that triggered the change (nullable)
     * @param recalculatedPillars Which pillars were recalculated (e.g., "p1,p2,p3" or "p4,p5,p6")
     * @return The saved ScoreHistory entry
     */
    public ScoreHistory createScoreHistory(
            Patient patient,
            int p1Score, int p2Score, int p3Score,
            int p4Score, int p5Score, int p6Score,
            RiskLevel pgsiRiskLevel,
            Integer pgsiScore,
            CalculationSource calculationSource,
            UUID triggerEntityId,
            String recalculatedPillars) {

        int totalScore = scoreCalculationService.calculateTotalScore(p1Score, p2Score, p3Score, p4Score, p5Score, p6Score);
        RiskLevel scoreRiskLevel = scoreCalculationService.deriveRiskLevelFromScore(totalScore);

        ScoreHistory history = new ScoreHistory();
        history.setPatient(patient);
        history.setTotalScore(totalScore);
        history.setP1Score(p1Score);
        history.setP2Score(p2Score);
        history.setP3Score(p3Score);
        history.setP4Score(p4Score);
        history.setP5Score(p5Score);
        history.setP6Score(p6Score);
        history.setScoreRiskLevel(scoreRiskLevel);
        history.setPgsiRiskLevel(pgsiRiskLevel);
        history.setPgsiScore(pgsiScore);
        history.setCalculationSource(calculationSource);
        history.setTriggerEntityId(triggerEntityId);
        history.setRecalculatedPillars(recalculatedPillars);

        return scoreHistoryRepository.save(history);
    }

    /**
     * Create score history entry for bet operations.
     * Preserves assessment pillars (p4-p6) from latest history.
     */
    public ScoreHistory createScoreHistoryForBet(
            Patient patient,
            BetPillarScores betScores,
            UUID betId) {

        // Get current assessment pillar values from latest history
        LatestPillarValues latest = getLatestPillarValues(patient);

        return createScoreHistory(
                patient,
                betScores.p1Score(), betScores.p2Score(), betScores.p3Score(),
                latest.p4, latest.p5, latest.p6,
                latest.pgsiRiskLevel,
                latest.pgsiScore,
                CalculationSource.BET_OPERATION,
                betId,
                "p1,p2,p3");
    }

    /**
     * Create score history entry for daily assessments.
     * Preserves bet pillars (p1-p3) from latest history.
     */
    public ScoreHistory createScoreHistoryForDailyAssessment(
            Patient patient,
            AssessmentPillarScores assessmentScores,
            UUID dailyAssessmentId) {

        // Get current bet pillar values from latest history
        LatestPillarValues latest = getLatestPillarValues(patient);

        return createScoreHistory(
                patient,
                latest.p1, latest.p2, latest.p3,
                assessmentScores.p4Score(), assessmentScores.p5Score(), assessmentScores.p6Score(),
                latest.pgsiRiskLevel,
                latest.pgsiScore,
                CalculationSource.DAILY_ASSESSMENT,
                dailyAssessmentId,
                "p4,p5,p6");
    }

    /**
     * Create score history entry for monthly assessments (PGSI).
     * Preserves ALL pillars (p1-p6) from latest history.
     * Only updates PGSI score and risk level - no pillar recalculation.
     * 
     * PGSI is a separate diagnostic metric that doesn't affect the 6 pillars.
     * The pillars are only recalculated by:
     * - Daily Assessment: recalculates p4-p6 (craving-based)
     * - Bet Operations: recalculates p1-p3 (bet history-based)
     */
    public ScoreHistory createScoreHistoryForMonthlyAssessment(
            Patient patient,
            int pgsiScore,
            RiskLevel pgsiRiskLevel,
            UUID monthlyAssessmentId) {

        // Get ALL current pillar values from latest history - preserve everything
        LatestPillarValues latest = getLatestPillarValues(patient);

        return createScoreHistory(
                patient,
                latest.p1, latest.p2, latest.p3,
                latest.p4, latest.p5, latest.p6,
                pgsiRiskLevel,
                pgsiScore,
                CalculationSource.MONTHLY_ASSESSMENT,
                monthlyAssessmentId,
                "none");
    }

    /**
     * Get the latest pillar values for a patient.
     * Returns default values if no history exists.
     */
    public LatestPillarValues getLatestPillarValues(Patient patient) {
        Optional<ScoreHistory> latestOpt = scoreHistoryRepository.findTopByPatientOrderByRecordedAtDesc(patient);

        if (latestOpt.isEmpty()) {
            return new LatestPillarValues(0, 0, 0, 0, 0, 0, null, null);
        }

        ScoreHistory latest = latestOpt.get();
        return new LatestPillarValues(
                latest.getP1Score(),
                latest.getP2Score(),
                latest.getP3Score(),
                latest.getP4Score(),
                latest.getP5Score(),
                latest.getP6Score(),
                latest.getPgsiRiskLevel(),
                latest.getPgsiScore());
    }

    /**
     * Record to hold latest pillar values from ScoreHistory.
     */
    public record LatestPillarValues(
            int p1, int p2, int p3,
            int p4, int p5, int p6,
            RiskLevel pgsiRiskLevel,
            Integer pgsiScore) {}

    /**
     * Save a ScoreHistory record.
     * Preserved for backward compatibility with existing code.
     */
    public void recordScore(ScoreHistory history) {
        scoreHistoryRepository.save(history);
    }

    /**
     * Find score history for patient within last 30 days.
     */
    public List<ScoreHistory> findByPatientLast30Days(Patient patient, LocalDateTime from) {
        return scoreHistoryRepository.findByPatientAndRecordedAtAfterOrderByRecordedAtDesc(patient, from);
    }

    /**
     * Find the top 2 most recent score history entries for a patient.
     */
    public List<ScoreHistory> findTop2ByPatient(Patient patient) {
        return scoreHistoryRepository.findTop2ByPatientOrderByRecordedAtDesc(patient);
    }

    /**
     * Find score history entries before or at a given date.
     */
    public List<ScoreHistory> findByPatientBeforeDate(Patient patient, LocalDateTime date) {
        return scoreHistoryRepository.findByPatientAndRecordedAtLessThanEqualOrderByRecordedAtDesc(patient, date);
    }

    /**
     * Get the latest ScoreHistory for a patient.
     */
    public Optional<ScoreHistory> getLatestScoreHistory(Patient patient) {
        return scoreHistoryRepository.findTopByPatientOrderByRecordedAtDesc(patient);
    }
}
