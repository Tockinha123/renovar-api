package com.tocka.renovarAPI.score.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.score.entity.ScoreHistory;

@Repository
public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, UUID> {
    
    List<ScoreHistory> findByPatientAndRecordedAtAfterOrderByRecordedAtDesc(Patient patient, LocalDateTime recordedAt);

    List<ScoreHistory> findTop2ByPatientOrderByRecordedAtDesc(Patient patient);

    List<ScoreHistory> findByPatientAndRecordedAtLessThanEqualOrderByRecordedAtDesc(Patient patient, LocalDateTime recordedAt);

    /**
     * Get the most recent score history entry for a patient.
     * Used to retrieve current pillar values when doing partial recalculations.
     */
    Optional<ScoreHistory> findTopByPatientOrderByRecordedAtDesc(Patient patient);
}
