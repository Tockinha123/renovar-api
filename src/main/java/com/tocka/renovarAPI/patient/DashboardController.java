package com.tocka.renovarAPI.patient;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tocka.renovarAPI.metrics.MetricsCalculatorService;
import com.tocka.renovarAPI.metrics.PatientMetrics;
import com.tocka.renovarAPI.metrics.PatientMetricsRepository;
import com.tocka.renovarAPI.user.User;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final PatientRepository patientRepository;
    private final PatientMetricsRepository metricsRepository;
    private final MetricsCalculatorService metricsCalculator;

    public DashboardController(PatientRepository patientRepository,
                               PatientMetricsRepository metricsRepository,
                               MetricsCalculatorService metricsCalculator) {
        this.patientRepository = patientRepository;
        this.metricsRepository = metricsRepository;
        this.metricsCalculator = metricsCalculator;
    }

    @GetMapping
    public ResponseEntity<DashboardDTO> getDashboard(@AuthenticationPrincipal User user) {
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

        return ResponseEntity.ok(dto);
    }
}
