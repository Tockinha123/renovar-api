package com.tocka.renovarAPI.patient;

import java.math.BigDecimal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tocka.renovarAPI.infra.exception.EmailAlreadyExistsException;
import com.tocka.renovarAPI.infra.security.TokenService;
import com.tocka.renovarAPI.metrics.MetricsCalculatorService;
import com.tocka.renovarAPI.metrics.PatientMetrics;
import com.tocka.renovarAPI.metrics.PatientMetricsRepository;
import com.tocka.renovarAPI.score.ScoreHistoryService;
import com.tocka.renovarAPI.user.LoginResponseDTO;
import com.tocka.renovarAPI.user.RegisterPatientDTO;
import com.tocka.renovarAPI.user.User;
import com.tocka.renovarAPI.user.UserRepository;
import com.tocka.renovarAPI.user.UserRole;

@Service
public class PatientService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PatientMetricsRepository patientMetricsRepository;
    private final PasswordEncoder passwordEncoder;
    private final MetricsCalculatorService metricsCalculator;
    private final TokenService tokenService;
    private final ScoreHistoryService scoreHistoryService;

    public PatientService(UserRepository userRepository,
                          PatientRepository patientRepository,
                          PatientMetricsRepository patientMetricsRepository,
                          PasswordEncoder passwordEncoder,
                          MetricsCalculatorService metricsCalculator,
                          TokenService tokenService,
                          ScoreHistoryService scoreHistoryService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.patientMetricsRepository = patientMetricsRepository;
        this.passwordEncoder = passwordEncoder;
        this.metricsCalculator = metricsCalculator;
        this.tokenService = tokenService;
        this.scoreHistoryService = scoreHistoryService;
    }

    @Transactional
    public LoginResponseDTO registerPatient(RegisterPatientDTO data) {
        // 1. Verificar se email já existe
        if (userRepository.findByEmail(data.email()).isPresent()) {
            throw new EmailAlreadyExistsException(data.email());
        }

        // 2. Criar e salvar User (autenticação)
        User user = new User();
        user.setEmail(data.email());
        user.setPassword(passwordEncoder.encode(data.password()));
        user.getRoles().add(UserRole.ROLE_PATIENT);
        userRepository.save(user);

        // 3. Criar e salvar Patient (dados pessoais + baselines)
        Patient patient = new Patient();
        patient.setUser(user);
        patient.setName(data.name());
        patient.setBirthDate(data.birthDate());
        patient.setFinancialBaseline(data.financialBaseline());
        patient.setSessionTimeBaseline(data.sessionTimeBaseline());
        patientRepository.save(patient);

        // 4. Criar e salvar PatientMetrics (score inicial = 500, riskLevel = BOM)
        PatientMetrics metrics = new PatientMetrics();
        metrics.setPatient(patient);
        metrics.setCurrentScore(500);
        metrics.setCurrentRiskLevel(metricsCalculator.calcularRiskLevel(500)); // BOM
        metrics.setCleanDaysStreak(0);
        metrics.setSavingsAccumulated(BigDecimal.ZERO);
        metrics.setTimeRecovered(0);
        patientMetricsRepository.save(metrics);

        // 4.1 Criar ScoreHistory inicial (NOVO)
        // Pilares: P1=290, P2=0, P3=210, P4=0, P5=0, P6=0 = 500
        scoreHistoryService.createInitialScoreHistory(patient);

        // 5. Gerar token e retornar
        var token = tokenService.generateToken(user);
        return new LoginResponseDTO(patient.getName(), user.getEmail(), token);
    }

    @Transactional(readOnly = true)
    public PatientProfileDTO getPatientProfile(User user) {
        Patient patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        
        return new PatientProfileDTO(patient);
    }

    @Transactional(readOnly = true)
    public String getPatientNameByUser(User user) {
        Patient patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        
        return patient.getName();
    }
}