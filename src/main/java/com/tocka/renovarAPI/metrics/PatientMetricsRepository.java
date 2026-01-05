package com.tocka.renovarAPI.metrics;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tocka.renovarAPI.patient.Patient;

public interface PatientMetricsRepository extends JpaRepository<PatientMetrics, UUID> {
    Optional<PatientMetrics> findByPatient(Patient patient);
}