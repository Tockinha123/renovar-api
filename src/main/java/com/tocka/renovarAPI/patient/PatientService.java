package com.tocka.renovarAPI.patient;

import java.math.BigDecimal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tocka.renovarAPI.infra.exception.EmailAlreadyExistsException;
import com.tocka.renovarAPI.metrics.PatientMetrics;
import com.tocka.renovarAPI.metrics.PatientMetricsRepository;
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

    public PatientService(UserRepository userRepository,
                          PatientRepository patientRepository,
                          PatientMetricsRepository patientMetricsRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.patientMetricsRepository = patientMetricsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Patient registerPatient(RegisterPatientDTO data) {
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

        // 4. Criar e salvar PatientMetrics (score inicial = 500)
        PatientMetrics metrics = new PatientMetrics();
        metrics.setPatient(patient);
        metrics.setCurrentScore(500);
        metrics.setCleanDaysStreak(0);
        metrics.setSavingsAccumulated(BigDecimal.ZERO);
        patientMetricsRepository.save(metrics);

        return patient;
    }
}