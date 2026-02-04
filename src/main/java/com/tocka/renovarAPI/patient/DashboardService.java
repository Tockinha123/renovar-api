package com.tocka.renovarAPI.patient;

import com.tocka.renovarAPI.metrics.MetricsCalculatorService;
import com.tocka.renovarAPI.metrics.PatientMetrics;
import com.tocka.renovarAPI.metrics.PatientMetricsRepository;
import com.tocka.renovarAPI.user.User;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private PatientRepository patientRepository;
    private PatientMetricsRepository metricsRepository;
    private MetricsCalculatorService metricsCalculator;

    public DashboardService(
            PatientRepository patientRepository,
            PatientMetricsRepository metricsRepository,
            MetricsCalculatorService metricsCalculator) {
        this.patientRepository = patientRepository;
        this.metricsRepository = metricsRepository;
        this.metricsCalculator = metricsCalculator;
    }

    @Transactional(readOnly = true) // Otimização para leitura
    public DashboardDTO gerarDashboard(User user) {
        // 1. Busca o Paciente
        Patient patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // 2. Busca as Métricas
        PatientMetrics metrics = metricsRepository.findByPatient(patient)
                .orElseThrow(() -> new RuntimeException("Métricas não encontradas"));

        long diasLimposStreak = metricsCalculator.calcularDiasLimpos(metrics);
        BigDecimal economiaStreak = metricsCalculator.calcularEconomia(metrics);
        long horasStreak = metricsCalculator.calcularHorasSalvas(metrics);

        // 2. O que ele já tinha guardado no COFRE (histórico)
        BigDecimal cofreDinheiro = metrics.getSavingsAccumulated() != null ? metrics.getSavingsAccumulated() : BigDecimal.ZERO;
        long cofreTempo = metrics.getTimeRecovered() != null ? metrics.getTimeRecovered() : 0;

        // 3. Soma para exibir o Total Vitalício
        BigDecimal economiaTotal = cofreDinheiro.add(economiaStreak);
        long horasTotais = cofreTempo + horasStreak;

        // 4. Monta o DTO
        DashboardDTO dto = new DashboardDTO(
            metrics.getCurrentScore(),
            metrics.getCurrentRiskLevel(),
            diasLimposStreak,
            economiaTotal,
            horasTotais,
            metrics.getCleanDaysStreak()
        );

        return dto;
    }   
}
