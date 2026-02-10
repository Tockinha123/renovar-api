package com.tocka.renovarAPI.score;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tocka.renovarAPI.assessment.repository.DailyAssessmentRepository;
import com.tocka.renovarAPI.bets.Bet;
import com.tocka.renovarAPI.bets.BetRepository;
import com.tocka.renovarAPI.metrics.PatientMetrics;
import com.tocka.renovarAPI.metrics.RiskLevel;
import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.score.model.AllPillarScores;

/**
 * Service responsável por calcular os 6 pilares do Score Re:Novar.
 * 
 * =====================================================================
 * MUDANÇA DE DESIGN (v2):
 * =====================================================================
 * 
 * ANTES: Aposta recalculava P1-P3 e COPIAVA P4-P6 do último registro.
 *        Avaliação recalculava P4-P6 e COPIAVA P1-P3 do último registro.
 *        → P3 ficava preso em 0 pra sempre após binge
 *        → P5 ficava em 80 pra sempre após 1 check-in
 *        → P4 não decaía quando paciente sumia do app
 * 
 * AGORA: Tanto aposta quanto avaliação recalculam TODOS os 6 pilares.
 *        → P3 recupera naturalmente pela janela de 30 dias
 *        → P5 usa janela de 7 dias (incentiva check-in diário)
 *        → P4 decai pro neutro quando sem check-in recente
 * 
 * =====================================================================
 * 
 * PILARES:
 *   P1 (Abstinência)   - max 290 - Dias limpos nos últimos 30 dias
 *   P2 (Streak)        - max 240 - Sequência ininterrupta sem apostar
 *   P3 (Financeiro)    - max 210 - Gravidade da aposta (ou ausência dela)
 *   P4 (Fissura)       - max 120 - Vontade de apostar (com decay temporal)
 *   P5 (Engajamento)   - max 80  - Check-ins nos últimos 7 dias
 *   P6 (Prevenção)     - max 60  - Reservado (futuro: ferramentas preventivas)
 */
@Service
public class ScoreCalculationService {

    private final BetRepository betRepository;
    private final DailyAssessmentRepository dailyAssessmentRepository;
    private final Clock clock;

    @Autowired
    public ScoreCalculationService(BetRepository betRepository,
                                   DailyAssessmentRepository dailyAssessmentRepository) {
        this(betRepository, dailyAssessmentRepository, Clock.systemDefaultZone());
    }

    // Construtor para testes (injeção de Clock)
    ScoreCalculationService(BetRepository betRepository,
                            DailyAssessmentRepository dailyAssessmentRepository,
                            Clock clock) {
        this.betRepository = betRepository;
        this.dailyAssessmentRepository = dailyAssessmentRepository;
        this.clock = clock;
    }

    // =====================================================================
    // MÉTODOS PRINCIPAIS - Cálculo completo dos 6 pilares
    // =====================================================================

    /**
     * Calcula TODOS os 6 pilares no contexto de uma AVALIAÇÃO DIÁRIA.
     * 
     * P1-P3: Recalculados com base no histórico de apostas (permite recuperação de P3).
     * P4: Calculado com base no craving reportado AGORA (valor fresco).
     * P5: Calculado pela janela de 7 dias de check-ins.
     * P6: Reservado (0).
     * 
     * @param patient O paciente
     * @param metrics Métricas do paciente (contém streak)
     * @param rawCravingLevel Nível de fissura raw (0-10, da label da pergunta)
     * @return AllPillarScores com todos os 6 pilares
     */
    public AllPillarScores calculateAllForDailyAssessment(
            Patient patient, PatientMetrics metrics, int rawCravingLevel) {

        int p1 = calculateP1(patient);
        int p2 = calculateP2(metrics);
        int p3 = calculateP3NoNewBet(patient);   // Permite recuperação temporal!
        int p4 = calculateP4(rawCravingLevel);    // Valor fresco do check-in de hoje
        int p5 = calculateP5(patient);            // Janela de 7 dias
        int p6 = 0;

        return new AllPillarScores(p1, p2, p3, p4, p5, p6);
    }

    /**
     * Calcula TODOS os 6 pilares no contexto de uma APOSTA sendo registrada.
     * 
     * P1-P2: Recalculados (P1 vai piorar, P2 sofre soft reset).
     * P3: Calculado com base no valor da aposta ATUAL.
     * P4: Usa decay temporal se não tem check-in recente.
     * P5: Calculado pela janela de 7 dias de check-ins.
     * P6: Reservado (0).
     * 
     * @param patient O paciente
     * @param metrics Métricas do paciente (contém streak e lastCheckin)
     * @param betAmount Valor da aposta sendo registrada
     * @param lastP4Value Último valor de P4 do ScoreHistory (para decay)
     * @return AllPillarScores com todos os 6 pilares
     */
    public AllPillarScores calculateAllForBet(
            Patient patient, PatientMetrics metrics,
            BigDecimal betAmount, int lastP4Value) {

        int p1 = calculateP1(patient);
        int p2 = calculateP2(metrics);
        int p3 = calculateP3ForBet(patient, betAmount);  // Avalia aposta atual
        int p4 = calculateP4WithDecay(metrics, lastP4Value); // Decay se sem check-in
        int p5 = calculateP5(patient);                    // Janela de 7 dias
        int p6 = 0;

        return new AllPillarScores(p1, p2, p3, p4, p5, p6);
    }

    // =====================================================================
    // PILAR 1 - Abstinência Recente (max 290)
    // =====================================================================

    /**
     * P1 = (diasLimpos / 30) × 290
     * 
     * Usa janela móvel de 30 dias. Uma recaída isolada não zera o pilar.
     * Exemplo: 29 dias limpos, 1 com aposta → P1 = (29/30) × 290 = 280.
     */
    int calculateP1(Patient patient) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime start = today.minusDays(29).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<Bet> betsLast30Days = betRepository.findByPatientAndCreatedAtBetween(patient, start, end);
        Set<LocalDate> daysWithBet = betsLast30Days.stream()
                .map(bet -> bet.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        int cleanDays = Math.max(0, 30 - daysWithBet.size());
        return Math.round((cleanDays / 30.0f) * 290);
    }

    // =====================================================================
    // PILAR 2 - Consistência / Streak (max 240)
    // =====================================================================

    /**
     * P2 = min(streak/30 × 240, 240)
     * 
     * Satura em 30 dias. O streak sofre "soft reset" (÷2) ao apostar,
     * preservando parte do progresso histórico.
     */
    int calculateP2(PatientMetrics metrics) {
        int streak = metrics.getCleanDaysStreak() == null ? 0 : metrics.getCleanDaysStreak();
        return Math.min(240, Math.round((streak / 30.0f) * 240));
    }

    // =====================================================================
    // PILAR 3 - Intensidade Financeira (max 210)
    // =====================================================================

    /**
     * P3 quando uma NOVA APOSTA está sendo registrada.
     * 
     * Apostar NUNCA dá 210. Valores possíveis:
     * - 150: aposta ≤ baseline (deslize leve)
     * - 0:   aposta > baseline (descontrole/binge)
     */
    int calculateP3ForBet(Patient patient, BigDecimal currentBetAmount) {
        BigDecimal baseline = getFinancialBaseline(patient);

        if (currentBetAmount == null || currentBetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 210; // Safety fallback
        }

        return currentBetAmount.compareTo(baseline) > 0 ? 0 : 150;
    }

    /**
     * P3 quando NÃO há aposta nova (contexto de avaliação diária ou recálculo).
     * 
     * MUDANÇA CHAVE: Permite recuperação temporal!
     * - Sem apostas nos últimos 30 dias → P3 = 210 (recuperou!)
     * - Com apostas, avalia a mais recente vs baseline.
     */
    int calculateP3NoNewBet(Patient patient) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime start = today.minusDays(29).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<Bet> betsLast30Days = betRepository.findByPatientAndCreatedAtBetween(patient, start, end);

        if (betsLast30Days.isEmpty()) {
            return 210; // Sem apostas nos últimos 30 dias → recuperou!
        }

        Bet mostRecentBet = betsLast30Days.stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElse(null);

        if (mostRecentBet == null) {
            return 210;
        }

        BigDecimal baseline = getFinancialBaseline(patient);
        return mostRecentBet.getAmount().compareTo(baseline) > 0 ? 0 : 150;
    }

    // =====================================================================
    // PILAR 4 - Fissura / Craving (max 120)
    // =====================================================================

    /**
     * P4 = 120 - (rawCraving × 12)
     * 
     * Usado quando o paciente ACABOU de reportar fissura no check-in.
     * rawCraving: 0 = nenhuma → P4 = 120, 10 = extrema → P4 = 0.
     * 
     * IMPORTANTE: rawCraving é o valor da LABEL (0-10), NÃO o score_value do banco.
     */
    public int calculateP4(int rawCraving) {
        int clamped = Math.max(0, Math.min(10, rawCraving));
        return 120 - (clamped * 12);
    }

    /**
     * P4 com DECAY temporal - usado quando NÃO houve check-in hoje.
     * 
     * MUDANÇA DE DESIGN: Se o paciente não faz check-in, o P4 decai 
     * gradualmente pro valor neutro (60 = fissura nível 5).
     * Isso impede que o paciente "congele" um P4 bom e suma do app.
     * 
     * Regras:
     * - Check-in hoje ou ontem: mantém o último P4 (ainda é "fresco")
     * - Sem check-in há 2+ dias: perde 15 pts/dia, até chegar em neutro (60)
     * - Se o último P4 já era ≤ 60 (fissura alta): mantém (não melhora sozinho)
     * - Nunca fez check-in: P4 = 0
     */
    int calculateP4WithDecay(PatientMetrics metrics, int lastP4Value) {
        LocalDateTime lastCheckin = metrics.getLastCheckin();

        // Nunca fez check-in → sem dados = 0
        if (lastCheckin == null) {
            return 0;
        }

        long daysSinceCheckin = ChronoUnit.DAYS.between(
                lastCheckin.toLocalDate(), LocalDate.now(clock));

        // Check-in recente (hoje ou ontem): mantém valor
        if (daysSinceCheckin <= 1) {
            return lastP4Value;
        }

        // Último P4 já estava ruim: não melhora sozinho
        int neutral = 60;
        if (lastP4Value <= neutral) {
            return lastP4Value;
        }

        // Decay: perde 15 pts por dia além do 1º dia, piso no neutro
        int decayed = lastP4Value - ((int)(daysSinceCheckin - 1) * 15);
        return Math.max(neutral, decayed);
    }

    // =====================================================================
    // PILAR 5 - Engajamento (max 80) 
    // =====================================================================

    /**
     * P5 = (checkInsNosÚltimos7Dias / 7) × 80
     * 
     * MUDANÇA DE DESIGN: Antes era binário (fez = 80, não fez = 0).
     * Agora usa janela móvel de 7 dias, incentivando check-in diário.
     * 
     * Exemplos:
     * - 7/7 check-ins → P5 = 80 (máximo)
     * - 4/7 check-ins → P5 = 45
     * - 1/7 check-ins → P5 = 11
     * - 0/7 check-ins → P5 = 0
     * 
     * REQUER: Método countByPatientAndAssessmentDateBetween no DailyAssessmentRepository.
     */
    int calculateP5(Patient patient) {
        LocalDate today = LocalDate.now(clock);
        LocalDate sevenDaysAgo = today.minusDays(6); // Hoje + 6 dias atrás = 7 dias

        long checkInsLast7Days = dailyAssessmentRepository
                .countByPatientAndAssessmentDateBetween(patient, sevenDaysAgo, today);

        // Clamp pra segurança (máx 7)
        long clamped = Math.min(7, checkInsLast7Days);
        return Math.round((clamped / 7.0f) * 80);
    }

    // =====================================================================
    // PILAR 6 - Prevenção (max 60) - RESERVADO
    // =====================================================================

    // P6 = 0 por enquanto. No futuro: uso de ferramentas preventivas
    // (botão de pânico, diário, leitura de conteúdo educativo).

    // =====================================================================
    // MÉTODOS AUXILIARES
    // =====================================================================

    /**
     * Baseline financeiro com fallback seguro.
     * Se null ou zero, usa valor alto pra não penalizar indevidamente.
     */
    private BigDecimal getFinancialBaseline(Patient patient) {
        BigDecimal baseline = patient.getFinancialBaseline();
        if (baseline == null || baseline.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("999999");
        }
        return baseline;
    }

    /**
     * Calcula score total a partir dos 6 pilares individuais.
     */
    public int calculateTotalScore(int p1, int p2, int p3, int p4, int p5, int p6) {
        return p1 + p2 + p3 + p4 + p5 + p6;
    }

    /**
     * Calcula score total a partir de AllPillarScores.
     */
    public int calculateTotalScore(AllPillarScores scores) {
        return scores.total();
    }

    public RiskLevel deriveRiskLevelFromScore(int score) {
        return RiskLevel.fromScore(score);
    }

    public RiskLevel calculatePgsiRiskLevel(int pgsiScore) {
        return RiskLevel.fromPgsi(pgsiScore);
    }

    /**
     * Calcula variação entre score anterior e atual.
     */
    public double calculateVariation(Integer previousScore, Integer currentScore) {
        if (previousScore == null || previousScore == 0) {
            return 0.0;
        }
        return ((double) currentScore - previousScore);
    }

    /**
     * Pilares iniciais para paciente novo (cadastro).
     * 
     * P1 = 290 (30/30 dias limpos, nunca apostou)
     * P2 = 0   (streak começa em 0)
     * P3 = 210 (nunca apostou)
     * P4 = 0   (sem check-in ainda)
     * P5 = 0   (sem check-ins na janela de 7 dias)
     * P6 = 0   (reservado)
     * Total = 500
     */
    public static AllPillarScores getInitialPillarScores() {
        return new AllPillarScores(290, 0, 210, 0, 0, 0);
    }
}