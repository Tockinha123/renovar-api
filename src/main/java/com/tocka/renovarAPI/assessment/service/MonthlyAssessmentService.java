package com.tocka.renovarAPI.assessment.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tocka.renovarAPI.assessment.dto.AnswerDTO;
import com.tocka.renovarAPI.assessment.dto.MonthlyAssessmentFeedbackDTO;
import com.tocka.renovarAPI.assessment.dto.MonthlyAssessmentQuestionsResponseDTO;
import com.tocka.renovarAPI.assessment.dto.QuestionDTO;
import com.tocka.renovarAPI.assessment.dto.QuestionOptionDTO;
import com.tocka.renovarAPI.assessment.dto.SubmitAssessmentRequestDTO;
import com.tocka.renovarAPI.assessment.entities.AssessmentAnswer;
import com.tocka.renovarAPI.assessment.entities.AssessmentOption;
import com.tocka.renovarAPI.assessment.entities.AssessmentQuestion;
import com.tocka.renovarAPI.assessment.entities.AssessmentType;
import com.tocka.renovarAPI.assessment.entities.MonthlyAssessment;
import com.tocka.renovarAPI.assessment.repository.AssessmentAnswerRepository;
import com.tocka.renovarAPI.assessment.repository.AssessmentOptionRepository;
import com.tocka.renovarAPI.assessment.repository.AssessmentQuestionRepository;
import com.tocka.renovarAPI.assessment.repository.MonthlyAssessmentRepository;
import com.tocka.renovarAPI.assessment.validation.AssessmentSubmissionValidator;
import com.tocka.renovarAPI.infra.exception.AssessmentAlreadySubmittedException;
import com.tocka.renovarAPI.metrics.PatientMetrics;
import com.tocka.renovarAPI.metrics.PatientMetricsRepository;
import com.tocka.renovarAPI.metrics.RiskLevel;
import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.patient.PatientRepository;
import com.tocka.renovarAPI.score.ScoreCalculationService;
import com.tocka.renovarAPI.score.ScoreHistoryService;
import com.tocka.renovarAPI.score.entity.ScoreHistory;
import com.tocka.renovarAPI.user.User;

@Service
public class MonthlyAssessmentService {

    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentOptionRepository optionRepository;
    private final MonthlyAssessmentRepository monthlyAssessmentRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final PatientRepository patientRepository;
    private final PatientMetricsRepository metricsRepository;
    private final ScoreCalculationService scoreCalculationService;
    private final ScoreHistoryService scoreHistoryService;
    private final AssessmentSubmissionValidator submissionValidator;

    public MonthlyAssessmentService(
            AssessmentQuestionRepository questionRepository,
            AssessmentOptionRepository optionRepository,
            MonthlyAssessmentRepository monthlyAssessmentRepository,
            AssessmentAnswerRepository answerRepository,
            PatientRepository patientRepository,
            PatientMetricsRepository metricsRepository,
            ScoreCalculationService scoreCalculationService,
            ScoreHistoryService scoreHistoryService,
            AssessmentSubmissionValidator submissionValidator) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.monthlyAssessmentRepository = monthlyAssessmentRepository;
        this.answerRepository = answerRepository;
        this.patientRepository = patientRepository;
        this.metricsRepository = metricsRepository;
        this.scoreCalculationService = scoreCalculationService;
        this.scoreHistoryService = scoreHistoryService;
        this.submissionValidator = submissionValidator;
    }

    public MonthlyAssessmentQuestionsResponseDTO getQuestionsWithStatus(User user) {
        Patient patient = getPatient(user);
        PatientMetrics metrics = getMetrics(patient);
        YearMonth now = YearMonth.now();

        List<QuestionDTO> questions = buildQuestions(AssessmentType.MONTHLY);

        if (!monthlyAssessmentRepository.existsByPatientAndReferenceMonthAndReferenceYear(
                patient,
                now.getMonthValue(),
                now.getYear())) {
            return new MonthlyAssessmentQuestionsResponseDTO(questions, null);
        }

        MonthlyAssessmentFeedbackDTO feedback = buildFeedback(patient, metrics);
        return new MonthlyAssessmentQuestionsResponseDTO(questions, feedback);
    }

    @Transactional
    public MonthlyAssessmentQuestionsResponseDTO submitAssessment(User user, SubmitAssessmentRequestDTO request) {
        Patient patient = getPatient(user);
        PatientMetrics metrics = getMetrics(patient);
        YearMonth now = YearMonth.now();

        if (monthlyAssessmentRepository.existsByPatientAndReferenceMonthAndReferenceYear(
                patient,
                now.getMonthValue(),
                now.getYear())) {
            throw new AssessmentAlreadySubmittedException("Avaliacao mensal ja enviada");
        }

        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new IllegalArgumentException("Answers are required");
        }

        submissionValidator.validateAnswers(AssessmentType.MONTHLY, request.answers());

        int pgsiScore = 0;
        List<AssessmentAnswer> answersToSave = new ArrayList<>();

        for (AnswerDTO answerDTO : request.answers()) {
            AssessmentQuestion question = questionRepository.findById(answerDTO.questionId())
                    .orElseThrow(() -> new RuntimeException("Pergunta nao encontrada"));
            if (question.getType() != AssessmentType.MONTHLY) {
                throw new RuntimeException("Pergunta invalida para avaliacao mensal");
            }

            AssessmentOption option = optionRepository.findById(answerDTO.optionId())
                    .orElseThrow(() -> new RuntimeException("Opcao nao encontrada"));
            if (!option.getQuestion().getId().equals(question.getId())) {
                throw new RuntimeException("Opcao nao corresponde a pergunta");
            }

            pgsiScore += option.getScoreValue();

            AssessmentAnswer answer = new AssessmentAnswer();
            answer.setQuestion(question);
            answer.setOption(option);
            answersToSave.add(answer);
        }

        MonthlyAssessment assessment = new MonthlyAssessment();
        assessment.setPatient(patient);
        assessment.setReferenceMonth(now.getMonthValue());
        assessment.setReferenceYear(now.getYear());
        assessment.setPgsiScore(pgsiScore);
        MonthlyAssessment savedAssessment = monthlyAssessmentRepository.save(assessment);

        for (AssessmentAnswer answer : answersToSave) {
            answer.setMonthlyAssessment(savedAssessment);
        }
        answerRepository.saveAll(answersToSave);

        // Calculate PGSI risk level
        RiskLevel pgsiRiskLevel = scoreCalculationService.calculatePgsiRiskLevel(pgsiScore);

        // Create score history entry preserving ALL pillars (p1-p6) from latest history
        // PGSI is a separate diagnostic metric - it does NOT recalculate any pillars
        // Pillars are only recalculated by:
        // - Daily Assessment: recalculates p4-p6 (craving-based)
        // - Bet Operations: recalculates p1-p3 (bet history-based)
        ScoreHistory history = scoreHistoryService.createScoreHistoryForMonthlyAssessment(
                patient,
                pgsiScore,
                pgsiRiskLevel,
                savedAssessment.getId());

        // Update metrics with PGSI risk level (monthly assessment updates PGSI, not score risk)
        metrics.setCurrentRiskLevel(pgsiRiskLevel);
        metrics.setCurrentScore(history.getTotalScore());
        metricsRepository.save(metrics);

        MonthlyAssessmentFeedbackDTO feedback = buildFeedback(patient, metrics);
        List<QuestionDTO> questions = buildQuestions(AssessmentType.MONTHLY);
        return new MonthlyAssessmentQuestionsResponseDTO(questions, feedback);
    }

    private Patient getPatient(User user) {
        return patientRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
    }

    private PatientMetrics getMetrics(Patient patient) {
        return metricsRepository.findByPatient(patient)
                .orElseThrow(() -> new RuntimeException("Métricas não encontradas"));
    }

    private List<QuestionDTO> buildQuestions(AssessmentType type) {
        return questionRepository.findByTypeAndActiveTrue(type).stream()
                .sorted(Comparator.comparing(AssessmentQuestion::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(question -> {
                    List<QuestionOptionDTO> options = optionRepository.findByQuestionAndActiveTrue(question).stream()
                            .sorted(Comparator.comparing(AssessmentOption::getScoreValue))
                            .map(option -> new QuestionOptionDTO(option.getId(), option.getLabel(), option.getScoreValue()))
                            .toList();
                    return new QuestionDTO(question.getId(), question.getTitle(), options);
                })
                .toList();
    }

    private MonthlyAssessmentFeedbackDTO buildFeedback(Patient patient, PatientMetrics metrics) {
        List<ScoreHistory> history = scoreHistoryService.findTop2ByPatient(patient);
        ScoreHistory latest = history.isEmpty() ? null : history.get(0);
        Integer currentScore = latest != null ? latest.getTotalScore() : metrics.getCurrentScore();
        if (currentScore == null) {
            currentScore = 0;
        }
        Integer previousScore = history.size() > 1 ? history.get(1).getTotalScore() : null;

        double variation = scoreCalculationService.calculateVariation(previousScore, currentScore);
        MonthlyAssessment latestMonthly = monthlyAssessmentRepository
                .findTopByPatientOrderByReferenceYearDescReferenceMonthDesc(patient)
                .orElse(null);
        Integer pgsiScore = latestMonthly != null ? latestMonthly.getPgsiScore() : null;
        if (pgsiScore == null) {
            pgsiScore = 0;
        }
        RiskLevel pgsiRiskLevel = latestMonthly != null
                ? scoreCalculationService.calculatePgsiRiskLevel(pgsiScore)
                : null;

        return new MonthlyAssessmentFeedbackDTO(pgsiScore + "/27", variation, pgsiRiskLevel);
    }

}
