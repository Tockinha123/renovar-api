package com.tocka.renovarAPI.score;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tocka.renovarAPI.metrics.RiskLevel;
import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.score.entity.ScoreHistory;
import com.tocka.renovarAPI.score.model.AllPillarScores;
import com.tocka.renovarAPI.score.model.CalculationSource;
import com.tocka.renovarAPI.score.repository.ScoreHistoryRepository;

/**
 * Service responsável por gerenciar o histórico de scores.
 * 
 * =====================================================================
 * MUDANÇA DE DESIGN (v2):
 * =====================================================================
 * 
 * Simplificado: tanto aposta quanto avaliação agora enviam AllPillarScores
 * (todos os 6 pilares). Não existe mais "copiar metade do último registro".
 * 
 * O único fluxo que ainda preserva pilares é a Avaliação Mensal (PGSI),
 * que não afeta nenhum pilar — só atualiza o score PGSI separado.
 * =====================================================================
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

    // =====================================================================
    // CRIAÇÃO DE REGISTROS
    // =====================================================================

    /**
     * Cria registro de score para APOSTA.
     * Recebe todos os 6 pilares já calculados.
     */
    public ScoreHistory createScoreHistoryForBet(
            Patient patient, AllPillarScores scores, UUID betId) {

        return createScoreHistory(
                patient, scores,
                getLatestPgsiRiskLevel(patient),
                getLatestPgsiScore(patient),
                CalculationSource.BET_OPERATION,
                betId,
                "p1,p2,p3,p4,p5,p6");
    }

    /**
     * Cria registro de score para AVALIAÇÃO DIÁRIA.
     * Recebe todos os 6 pilares já calculados.
     */
    public ScoreHistory createScoreHistoryForDailyAssessment(
            Patient patient, AllPillarScores scores, UUID dailyAssessmentId) {

        return createScoreHistory(
                patient, scores,
                getLatestPgsiRiskLevel(patient),
                getLatestPgsiScore(patient),
                CalculationSource.DAILY_ASSESSMENT,
                dailyAssessmentId,
                "p1,p2,p3,p4,p5,p6");
    }

    /**
     * Cria registro de score para AVALIAÇÃO MENSAL (PGSI).
     * 
     * Preserva TODOS os pilares do último registro.
     * PGSI é métrica diagnóstica separada — não afeta os 6 pilares.
     */
    public ScoreHistory createScoreHistoryForMonthlyAssessment(
            Patient patient, int pgsiScore, RiskLevel pgsiRiskLevel,
            UUID monthlyAssessmentId) {

        LatestPillarValues latest = getLatestPillarValues(patient);

        return createScoreHistory(
                patient,
                new AllPillarScores(latest.p1, latest.p2, latest.p3,
                        latest.p4, latest.p5, latest.p6),
                pgsiRiskLevel,
                pgsiScore,
                CalculationSource.MONTHLY_ASSESSMENT,
                monthlyAssessmentId,
                "none");
    }

    /**
     * Cria registro INICIAL no cadastro do paciente.
     * 
     * Garante que getLatestPillarValues() tem dados reais desde o dia 1.
     * Pilares: P1=290, P2=0, P3=210, P4=0, P5=0, P6=0 = 500 total.
     */
    public ScoreHistory createInitialScoreHistory(Patient patient) {
        AllPillarScores initial = ScoreCalculationService.getInitialPillarScores();
        return createScoreHistory(
                patient, initial,
                null, null,
                CalculationSource.MANUAL_RECALCULATION,
                null,
                "p1,p2,p3,p4,p5,p6");
    }

    // =====================================================================
    // CONSULTAS
    // =====================================================================

    /**
     * Busca os últimos valores dos pilares para um paciente.
     * Retorna defaults consistentes com score 500 se não houver histórico.
     */
    public LatestPillarValues getLatestPillarValues(Patient patient) {
        Optional<ScoreHistory> latestOpt = scoreHistoryRepository
                .findTopByPatientOrderByRecordedAtDesc(patient);

        if (latestOpt.isEmpty()) {
            AllPillarScores initial = ScoreCalculationService.getInitialPillarScores();
            return new LatestPillarValues(
                    initial.p1(), initial.p2(), initial.p3(),
                    initial.p4(), initial.p5(), initial.p6(),
                    null, null);
        }

        ScoreHistory latest = latestOpt.get();
        return new LatestPillarValues(
                latest.getP1Score(), latest.getP2Score(), latest.getP3Score(),
                latest.getP4Score(), latest.getP5Score(), latest.getP6Score(),
                latest.getPgsiRiskLevel(), latest.getPgsiScore());
    }

    public Optional<ScoreHistory> getLatestScoreHistory(Patient patient) {
        return scoreHistoryRepository.findTopByPatientOrderByRecordedAtDesc(patient);
    }

    public List<ScoreHistory> findTop2ByPatient(Patient patient) {
        return scoreHistoryRepository.findTop2ByPatientOrderByRecordedAtDesc(patient);
    }

    public List<ScoreHistory> findByPatientLast30Days(Patient patient, LocalDateTime from) {
        return scoreHistoryRepository.findByPatientAndRecordedAtAfterOrderByRecordedAtDesc(patient, from);
    }

    public List<ScoreHistory> findByPatientBeforeDate(Patient patient, LocalDateTime date) {
        return scoreHistoryRepository.findByPatientAndRecordedAtLessThanEqualOrderByRecordedAtDesc(patient, date);
    }

    public void recordScore(ScoreHistory history) {
        scoreHistoryRepository.save(history);
    }

    // =====================================================================
    // HELPERS INTERNOS
    // =====================================================================

    /**
     * Método base que cria e salva um ScoreHistory.
     */
    private ScoreHistory createScoreHistory(
            Patient patient, AllPillarScores scores,
            RiskLevel pgsiRiskLevel, Integer pgsiScore,
            CalculationSource source, UUID triggerEntityId,
            String recalculatedPillars) {

        int totalScore = scores.total();
        RiskLevel scoreRiskLevel = scoreCalculationService.deriveRiskLevelFromScore(totalScore);

        ScoreHistory history = new ScoreHistory();
        history.setPatient(patient);
        history.setTotalScore(totalScore);
        history.setP1Score(scores.p1());
        history.setP2Score(scores.p2());
        history.setP3Score(scores.p3());
        history.setP4Score(scores.p4());
        history.setP5Score(scores.p5());
        history.setP6Score(scores.p6());
        history.setScoreRiskLevel(scoreRiskLevel);
        history.setPgsiRiskLevel(pgsiRiskLevel);
        history.setPgsiScore(pgsiScore);
        history.setCalculationSource(source);
        history.setTriggerEntityId(triggerEntityId);
        history.setRecalculatedPillars(recalculatedPillars);

        return scoreHistoryRepository.save(history);
    }

    /**
     * Busca último PGSI risk level preservando pra registros que não são PGSI.
     */
    private RiskLevel getLatestPgsiRiskLevel(Patient patient) {
        return getLatestPillarValues(patient).pgsiRiskLevel();
    }

    /**
     * Busca último PGSI score preservando pra registros que não são PGSI.
     */
    private Integer getLatestPgsiScore(Patient patient) {
        return getLatestPillarValues(patient).pgsiScore();
    }

    /**
     * Record com os últimos valores dos pilares.
     */
    public record LatestPillarValues(
            int p1, int p2, int p3,
            int p4, int p5, int p6,
            RiskLevel pgsiRiskLevel,
            Integer pgsiScore) {}
}