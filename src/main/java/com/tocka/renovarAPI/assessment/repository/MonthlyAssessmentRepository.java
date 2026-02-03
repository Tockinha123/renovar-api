package com.tocka.renovarAPI.assessment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.assessment.entities.MonthlyAssessment;
import com.tocka.renovarAPI.patient.Patient;

@Repository
public interface MonthlyAssessmentRepository extends JpaRepository<MonthlyAssessment, UUID> {
    boolean existsByPatientAndReferenceMonthAndReferenceYear(Patient patient, Integer referenceMonth, Integer referenceYear);

    Optional<MonthlyAssessment> findByPatientAndReferenceMonthAndReferenceYear(Patient patient, Integer referenceMonth, Integer referenceYear);

    Optional<MonthlyAssessment> findTopByPatientOrderByReferenceYearDescReferenceMonthDesc(Patient patient);
}
