package com.tocka.renovarAPI.assessment.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.assessment.entities.DailyAssessment;
import com.tocka.renovarAPI.patient.Patient;

@Repository
public interface DailyAssessmentRepository extends JpaRepository<DailyAssessment, UUID>, JpaSpecificationExecutor<DailyAssessment> {
    boolean existsByPatientAndAssessmentDate(Patient patient, LocalDate assessmentDate);

    Optional<DailyAssessment> findByPatientAndAssessmentDate(Patient patient, LocalDate assessmentDate);

    long countByPatientAndAssessmentDateBetween(Patient patient, LocalDate from, LocalDate to);
}
