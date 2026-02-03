package com.tocka.renovarAPI.assessment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.assessment.entities.ScoreHistory;
import com.tocka.renovarAPI.patient.Patient;

@Repository
public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, UUID> {
    List<ScoreHistory> findByPatientAndRecordedAtAfterOrderByRecordedAtDesc(Patient patient, LocalDateTime recordedAt);

    List<ScoreHistory> findTop2ByPatientOrderByRecordedAtDesc(Patient patient);
}
