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
import com.tocka.renovarAPI.assessment.entities.ScoreHistory;
import com.tocka.renovarAPI.assessment.repository.AssessmentAnswerRepository;
import com.tocka.renovarAPI.assessment.repository.AssessmentOptionRepository;
import com.tocka.renovarAPI.assessment.repository.AssessmentQuestionRepository;
import com.tocka.renovarAPI.assessment.repository.MonthlyAssessmentRepository;
import com.tocka.renovarAPI.assessment.repository.ScoreHistoryRepository;
import com.tocka.renovarAPI.assessment.validation.AssessmentSubmissionValidator;
import com.tocka.renovarAPI.infra.exception.AssessmentAlreadySubmittedException;
import com.tocka.renovarAPI.metrics.PatientMetrics;
import com.tocka.renovarAPI.metrics.PatientMetricsRepository;
import com.tocka.renovarAPI.metrics.RiskLevel;
import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.patient.PatientRepository;
import com.tocka.renovarAPI.user.User;

@Service
public class MonthlyAssessmentService {

    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentOptionRepository optionRepository;
    private final MonthlyAssessmentRepository monthlyAssessmentRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final PatientRepository patientRepository;
    private final PatientMetricsRepository metricsRepository;
    private final AssessmentScoringService scoringService;
    private final ScoreHistoryService scoreHistoryService;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final AssessmentSubmissionValidator submissionValidator;

    public MonthlyAssessmentService(
            AssessmentQuestionRepository questionRepository,
            AssessmentOptionRepository optionRepository,
            MonthlyAssessmentRepository monthlyAssessmentRepository,
            AssessmentAnswerRepository answerRepository,
            PatientRepository patientRepository,
            PatientMetricsRepository metricsRepository,
            AssessmentScoringService scoringService,
            ScoreHistoryService scoreHistoryService,
            ScoreHistoryRepository scoreHistoryRepository,
            AssessmentSubmissionValidator submissionValidator) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.monthlyAssessmentRepository = monthlyAssessmentRepository;
        this.answerRepository = answerRepository;
        this.patientRepository = patientRepository;
        this.metricsRepository = metricsRepository;
        this.scoringService = scoringService;
        this.scoreHistoryService = scoreHistoryService;
        this.scoreHistoryRepository = scoreHistoryRepository;
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
            throw new AssessmentAlreadySubmittedException("Avaliação mensal já enviada");
        }

        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new IllegalArgumentException("Answers are required");
        }

        submissionValidator.validateAnswers(AssessmentType.MONTHLY, request.answers());

        int pgsiScore = 0;
        List<AssessmentAnswer> answersToSave = new ArrayList<>();

        for (AnswerDTO answerDTO : request.answers()) {
            AssessmentQuestion question = questionRepository.findById(answerDTO.questionId())
                    .orElseThrow(() -> new RuntimeException("Pergunta não encontrada"));
            if (question.getType() != AssessmentType.MONTHLY) {
                throw new RuntimeException("Pergunta inválida para avaliação mensal");
            }

            AssessmentOption option = optionRepository.findById(answerDTO.optionId())
                    .orElseThrow(() -> new RuntimeException("Opção não encontrada"));
            if (!option.getQuestion().getId().equals(question.getId())) {
                throw new RuntimeException("Opção não corresponde à pergunta");
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

        RiskLevel pgsiRiskLevel = scoringService.calculatePgsiRiskLevel(pgsiScore);
        metrics.setCurrentRiskLevel(pgsiRiskLevel);
        metricsRepository.save(metrics);

        ScoreHistory latestHistory = scoreHistoryRepository.findTop2ByPatientOrderByRecordedAtDesc(patient)
                .stream()
                .findFirst()
                .orElse(null);

        int totalScore = metrics.getCurrentScore() != null ? metrics.getCurrentScore() : 0;
        int p1Score = latestHistory != null ? latestHistory.getP1Score() : 0;
        int p2Score = latestHistory != null ? latestHistory.getP2Score() : 0;
        int p3Score = latestHistory != null ? latestHistory.getP3Score() : 0;
        int p4Score = latestHistory != null ? latestHistory.getP4Score() : 0;
        int p5Score = latestHistory != null ? latestHistory.getP5Score() : 0;
        int p6Score = latestHistory != null ? latestHistory.getP6Score() : 0;
        RiskLevel scoreRiskLevel = latestHistory != null && latestHistory.getScoreRiskLevel() != null
                ? latestHistory.getScoreRiskLevel()
                : scoringService.deriveRiskLevelFromScore(totalScore);

        ScoreHistory history = new ScoreHistory();
        history.setPatient(patient);
        history.setTotalScore(totalScore);
        history.setP1Score(p1Score);
        history.setP2Score(p2Score);
        history.setP3Score(p3Score);
        history.setP4Score(p4Score);
        history.setP5Score(p5Score);
        history.setP6Score(p6Score);
        history.setScoreRiskLevel(scoreRiskLevel);
        history.setPgsiRiskLevel(pgsiRiskLevel);
        scoreHistoryService.recordScore(history);

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
        List<ScoreHistory> history = scoreHistoryRepository.findTop2ByPatientOrderByRecordedAtDesc(patient);
        ScoreHistory latest = history.isEmpty() ? null : history.get(0);
        Integer currentScore = latest != null ? latest.getTotalScore() : metrics.getCurrentScore();
        if (currentScore == null) {
            currentScore = 0;
        }
        Integer previousScore = history.size() > 1 ? history.get(1).getTotalScore() : null;

        double variation = scoringService.calculateVariation(previousScore, currentScore);
        MonthlyAssessment latestMonthly = monthlyAssessmentRepository
                .findTopByPatientOrderByReferenceYearDescReferenceMonthDesc(patient)
                .orElse(null);
        Integer pgsiScore = latestMonthly != null ? latestMonthly.getPgsiScore() : null;
        if (pgsiScore == null) {
            pgsiScore = 0;
        }
        RiskLevel pgsiRiskLevel = latestMonthly != null
                ? scoringService.calculatePgsiRiskLevel(pgsiScore)
                : null;

        return new MonthlyAssessmentFeedbackDTO(pgsiScore + "/27", variation, pgsiRiskLevel);
    }

}
