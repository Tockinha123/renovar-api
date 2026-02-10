package com.tocka.renovarAPI.bets;

import com.tocka.renovarAPI.metrics.MetricsCalculatorService;
import com.tocka.renovarAPI.metrics.PatientMetricsRepository;
import com.tocka.renovarAPI.patient.PatientRepository;
import com.tocka.renovarAPI.score.ScoreCalculationService;
import com.tocka.renovarAPI.score.ScoreHistoryService;
import com.tocka.renovarAPI.score.entity.ScoreHistory;
import com.tocka.renovarAPI.score.model.AllPillarScores;
import com.tocka.renovarAPI.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BetService {
    
    private final BetRepository betRepository;
    private final PatientRepository patientRepository;
    private final PatientMetricsRepository metricsRepository;
    private final MetricsCalculatorService calculator;
    private final ScoreCalculationService scoreCalculationService;
    private final ScoreHistoryService scoreHistoryService;

    public BetService(BetRepository betRepository,
                      PatientRepository patientRepository,
                      PatientMetricsRepository metricsRepository,
                      MetricsCalculatorService calculator,
                      ScoreCalculationService scoreCalculationService,
                      ScoreHistoryService scoreHistoryService) {
        this.betRepository = betRepository;
        this.patientRepository = patientRepository;
        this.metricsRepository = metricsRepository;
        this.calculator = calculator;
        this.scoreCalculationService = scoreCalculationService;
        this.scoreHistoryService = scoreHistoryService;
    }

    @Transactional
    public BetResponseDTO registrarAposta(User user, BetRequestDTO dados) {
        // 1. Buscas
        var patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        
        var metrics = metricsRepository.findByPatient(patient)
                .orElseThrow(() -> new RuntimeException("Métricas não encontradas"));

        // 2. Montar e Salvar Aposta
        var bet = Bet.builder()
                .patient(patient)
                .amount(dados.amount())
                .won(dados.won())
                .sessionTime(dados.sessionTime())
                .category(dados.category())
                .build();
        
        betRepository.save(bet); // Agora temos o ID gerado e a data @PrePersist

        // 3. Lógica do Cofre (Acumular histórico antes de resetar)
        BigDecimal economiaDaStreak = calculator.calcularEconomia(metrics);
        long horasDaStreak = calculator.calcularHorasSalvas(metrics);

        BigDecimal cofreDinheiro = metrics.getSavingsAccumulated() != null ? metrics.getSavingsAccumulated() : BigDecimal.ZERO;
        metrics.setSavingsAccumulated(cofreDinheiro.add(economiaDaStreak));

        Integer cofreTempo = metrics.getTimeRecovered() != null ? metrics.getTimeRecovered() : 0;
        metrics.setTimeRecovered(cofreTempo + (int) horasDaStreak);

        // 4. Reset e Penalidades
        metrics.setLastBetAt(LocalDateTime.now());
        
        int novaStreak = calculator.aplicarSoftReset(metrics.getCleanDaysStreak());
        metrics.setCleanDaysStreak(novaStreak);

        // 5. Calcular TODOS os 6 pilares (MUDANÇA v2)
        // Buscar último P4 pra calcular decay
        ScoreHistoryService.LatestPillarValues latestPillars = scoreHistoryService.getLatestPillarValues(patient);
        
        AllPillarScores scores = scoreCalculationService.calculateAllForBet(
                patient,
                metrics,
                dados.amount(),           // P3 avalia a aposta ATUAL
                latestPillars.p4()        // Último P4 pra decay
        );

        // Criar registro no histórico com todos os 6 pilares
        ScoreHistory history = scoreHistoryService.createScoreHistoryForBet(
                patient, scores, bet.getId());

        // 6. Atualizar métricas com novo score
        metrics.setCurrentScore(history.getTotalScore());
        metrics.setCurrentRiskLevel(history.getScoreRiskLevel());

        metricsRepository.save(metrics);

        // 7. Retorna o DTO da aposta criada
        return new BetResponseDTO(bet);
    }

    @Transactional(readOnly = true)
    public Page<BetResponseDTO> listarApostas(User user, Pageable pageable) {
        var patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        return betRepository.findByPatientOrderByCreatedAtDesc(patient, pageable)
                .map(BetResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public List<CalendarResponseDTO> gerarCalendario(User user, int month, int year) {
        
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Mês deve estar entre 1 e 12");
        }

        if (year < 2020 || year > LocalDate.now().getYear() + 1) {
            throw new IllegalArgumentException("Ano inválido");
        }
        
        var patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        YearMonth anoMes = YearMonth.of(year, month);
        LocalDateTime inicioMes = anoMes.atDay(1).atStartOfDay();
        LocalDateTime fimMes = anoMes.atEndOfMonth().atTime(23, 59, 59);

        List<Bet> apostasDoMes = betRepository.findByPatientAndCreatedAtBetween(patient, inicioMes, fimMes);

        Set<LocalDate> diasComAposta = apostasDoMes.stream()
                .map(bet -> bet.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        List<CalendarResponseDTO> calendario = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        for (int dia = 1; dia <= anoMes.lengthOfMonth(); dia++) {
            LocalDate dataAtual = anoMes.atDay(dia);
            DayStatus status;

            if (dataAtual.isAfter(hoje)) {
                status = DayStatus.FUTURO; 
            } else if (diasComAposta.contains(dataAtual)) {
                status = DayStatus.APOSTOU; 
            } else {
                status = DayStatus.LIMPO; 
            }

            calendario.add(new CalendarResponseDTO(dataAtual, status));
        }

        return calendario;
    }
}
