package com.tocka.renovarAPI.assessment.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tocka.renovarAPI.assessment.entities.ScoreHistory;
import com.tocka.renovarAPI.assessment.repository.ScoreHistoryRepository;
import com.tocka.renovarAPI.patient.Patient;

@Service
public class ScoreHistoryService {

    private final ScoreHistoryRepository scoreHistoryRepository;

    public ScoreHistoryService(ScoreHistoryRepository scoreHistoryRepository) {
        this.scoreHistoryRepository = scoreHistoryRepository;
    }

    public ScoreHistory recordScore(ScoreHistory scoreHistory) {
        return scoreHistoryRepository.save(scoreHistory);
    }

    public List<ScoreHistory> findByPatientLast30Days(Patient patient, LocalDateTime since) {
        return scoreHistoryRepository.findByPatientAndRecordedAtAfterOrderByRecordedAtDesc(patient, since);
    }
}
