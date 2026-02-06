package com.tocka.renovarAPI.score.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.score.entity.ScoreHistory;

@Repository
public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, UUID> {
    
    List<ScoreHistory> findByPatientAndRecordedAtAfterOrderByRecordedAtDesc(Patient patient, LocalDateTime recordedAt);

    List<ScoreHistory> findTop2ByPatientOrderByRecordedAtDesc(Patient patient);

    List<ScoreHistory> findByPatientAndRecordedAtLessThanEqualOrderByRecordedAtDesc(Patient patient, LocalDateTime recordedAt);


    Optional<ScoreHistory> findTopByPatientOrderByRecordedAtDesc(Patient patient);


    @Query("SELECT AVG(sh.totalScore) FROM ScoreHistory sh " +
           "WHERE sh.patient = :patient " +
           "AND YEAR(sh.recordedAt) = :year " +
           "AND MONTH(sh.recordedAt) = :month")
    Optional<Double> findAverageScoreByPatientAndMonth(
            @Param("patient") Patient patient,
            @Param("year") int year,
            @Param("month") int month);


    @Query("SELECT sh FROM ScoreHistory sh " +
           "WHERE sh.patient = :patient " +
           "AND YEAR(sh.recordedAt) = :year " +
           "AND MONTH(sh.recordedAt) = :month " +
           "ORDER BY sh.recordedAt DESC")
    List<ScoreHistory> findByPatientAndMonth(
            @Param("patient") Patient patient,
            @Param("year") int year,
            @Param("month") int month);
}