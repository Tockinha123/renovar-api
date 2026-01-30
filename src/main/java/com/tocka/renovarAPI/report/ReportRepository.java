package com.tocka.renovarAPI.report;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.patient.Patient;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    
    List<Report> findByPatientOrderByReferenceYearDescReferenceMonthDesc(Patient patient);
    
    Optional<Report> findByPatientAndReferenceMonthAndReferenceYear(
        Patient patient, Integer month, Integer year);
    
    Optional<Report> findByIdAndPatient(UUID id, Patient patient);
    
    boolean existsByPatientAndReferenceMonthAndReferenceYear(
        Patient patient, Integer month, Integer year);
}