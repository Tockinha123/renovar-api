package com.tocka.renovarAPI.patient;

import com.tocka.renovarAPI.metrics.MetricsCalculatorService;
import com.tocka.renovarAPI.metrics.PatientMetrics;
import com.tocka.renovarAPI.metrics.PatientMetricsRepository;
import com.tocka.renovarAPI.user.User;
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

        // 3. Calcula métricas em tempo real usando o serviço
        long diasLimpos = metricsCalculator.calcularDiasLimpos(metrics);
        var economiaAcumulada = metricsCalculator.calcularEconomia(metrics);
        long horasSalvas = metricsCalculator.calcularHorasSalvas(metrics);

        // 4. Monta o DTO
        DashboardDTO dto = new DashboardDTO(
            patient.getName(),
            metrics.getCurrentScore(),
            metrics.getCurrentRiskLevel() != null ? metrics.getCurrentRiskLevel().name() : "N/A",
            diasLimpos,
            economiaAcumulada,
            horasSalvas,
            metrics.getCleanDaysStreak()
        );

        return dto;
    }   
}
