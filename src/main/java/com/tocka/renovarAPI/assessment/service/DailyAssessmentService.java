package com.tocka.renovarAPI.assessment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tocka.renovarAPI.assessment.dto.AnswerDTO;
import com.tocka.renovarAPI.assessment.dto.AssessmentAnswerDTO;
import com.tocka.renovarAPI.assessment.dto.DailyAssessmentFeedbackDTO;
import com.tocka.renovarAPI.assessment.dto.DailyAssessmentHistoryDTO;
import com.tocka.renovarAPI.assessment.dto.DailyAssessmentQuestionsResponseDTO;
import com.tocka.renovarAPI.assessment.dto.QuestionDTO;
import com.tocka.renovarAPI.assessment.dto.QuestionOptionDTO;
import com.tocka.renovarAPI.assessment.dto.SubmitAssessmentRequestDTO;
import com.tocka.renovarAPI.assessment.entities.AssessmentAnswer;
import com.tocka.renovarAPI.assessment.entities.AssessmentOption;
import com.tocka.renovarAPI.assessment.entities.AssessmentQuestion;
import com.tocka.renovarAPI.assessment.entities.AssessmentType;
import com.tocka.renovarAPI.assessment.entities.DailyAssessment;
import com.tocka.renovarAPI.assessment.filter.DailyAssessmentFilter;
import com.tocka.renovarAPI.assessment.specification.DailyAssessmentSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tocka.renovarAPI.assessment.repository.AssessmentAnswerRepository;
import com.tocka.renovarAPI.assessment.repository.AssessmentOptionRepository;
import com.tocka.renovarAPI.assessment.repository.AssessmentQuestionRepository;
import com.tocka.renovarAPI.assessment.repository.DailyAssessmentRepository;
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
import com.tocka.renovarAPI.score.model.AssessmentPillarScores;
import com.tocka.renovarAPI.user.User;

@Service
public class DailyAssessmentService {

    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentOptionRepository optionRepository;
    private final DailyAssessmentRepository dailyAssessmentRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final PatientRepository patientRepository;
    private final PatientMetricsRepository metricsRepository;
    private final ScoreCalculationService scoreCalculationService;
    private final ScoreHistoryService scoreHistoryService;
    private final AssessmentSubmissionValidator submissionValidator;

    public DailyAssessmentService(
            AssessmentQuestionRepository questionRepository,
            AssessmentOptionRepository optionRepository,
            DailyAssessmentRepository dailyAssessmentRepository,
            MonthlyAssessmentRepository monthlyAssessmentRepository,
            AssessmentAnswerRepository answerRepository,
            PatientRepository patientRepository,
            PatientMetricsRepository metricsRepository,
            ScoreCalculationService scoreCalculationService,
            ScoreHistoryService scoreHistoryService,
            AssessmentSubmissionValidator submissionValidator) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.dailyAssessmentRepository = dailyAssessmentRepository;
        this.answerRepository = answerRepository;
        this.patientRepository = patientRepository;
        this.metricsRepository = metricsRepository;
        this.scoreCalculationService = scoreCalculationService;
        this.scoreHistoryService = scoreHistoryService;
        this.submissionValidator = submissionValidator;
    }

    public DailyAssessmentQuestionsResponseDTO getQuestionsWithStatus(User user) {
        Patient patient = getPatient(user);
        PatientMetrics metrics = getMetrics(patient);

        List<QuestionDTO> questions = buildQuestions(AssessmentType.DAILY);
        LocalDate today = LocalDate.now();

        if (!dailyAssessmentRepository.existsByPatientAndAssessmentDate(patient, today)) {
            return new DailyAssessmentQuestionsResponseDTO(questions, null);
        }

        DailyAssessmentFeedbackDTO feedback = buildFeedback(patient, metrics);
        return new DailyAssessmentQuestionsResponseDTO(questions, feedback);
    }

    @Transactional
    public DailyAssessmentQuestionsResponseDTO submitAssessment(User user, SubmitAssessmentRequestDTO request) {
        Patient patient = getPatient(user);
        PatientMetrics metrics = getMetrics(patient);
        LocalDate today = LocalDate.now();

        if (dailyAssessmentRepository.existsByPatientAndAssessmentDate(patient, today)) {
            throw new AssessmentAlreadySubmittedException("Avaliação diária já enviada");
        }

        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new IllegalArgumentException("Answers are required");
        }

        submissionValidator.validateAnswers(AssessmentType.DAILY, request.answers());

        Integer cravingLevel = null;
        List<AssessmentAnswer> answersToSave = new ArrayList<>();

        for (AnswerDTO answerDTO : request.answers()) {
            AssessmentQuestion question = questionRepository.findById(answerDTO.questionId())
                    .orElseThrow(() -> new RuntimeException("Pergunta não encontrada"));
            if (question.getType() != AssessmentType.DAILY) {
                throw new RuntimeException("Pergunta inválida para avaliação diária");
            }

            AssessmentOption option = optionRepository.findById(answerDTO.optionId())
                    .orElseThrow(() -> new RuntimeException("Opção não encontrada"));
            if (!option.getQuestion().getId().equals(question.getId())) {
                throw new RuntimeException("Opção não corresponde à pergunta");
            }

            cravingLevel = option.getScoreValue();

            AssessmentAnswer answer = new AssessmentAnswer();
            answer.setQuestion(question);
            answer.setOption(option);
            answersToSave.add(answer);
        }

        if (cravingLevel == null) {
            throw new RuntimeException("Craving não informado");
        }

        DailyAssessment assessment = new DailyAssessment();
        assessment.setPatient(patient);
        assessment.setAssessmentDate(today);
        assessment.setCravingLevel(cravingLevel);
        DailyAssessment savedAssessment = dailyAssessmentRepository.save(assessment);

        for (AssessmentAnswer answer : answersToSave) {
            answer.setDailyAssessment(savedAssessment);
        }
        answerRepository.saveAll(answersToSave);

        // Calculate only assessment pillars (p4-p6), preserving bet pillars (p1-p3)
        AssessmentPillarScores assessmentScores = scoreCalculationService.calculateAssessmentPillars(cravingLevel);
        
        // Create score history entry, preserving bet pillars from latest history
        ScoreHistory history = scoreHistoryService.createScoreHistoryForDailyAssessment(
                patient, 
                assessmentScores, 
                savedAssessment.getId());

        // Update metrics with new score and risk level
        metrics.setCurrentScore(history.getTotalScore());
        metrics.setCurrentRiskLevel(history.getScoreRiskLevel());
        metrics.setLastCheckin(LocalDateTime.now());
        metricsRepository.save(metrics);

        DailyAssessmentFeedbackDTO feedback = buildFeedback(patient, metrics);
        List<QuestionDTO> questions = buildQuestions(AssessmentType.DAILY);
        return new DailyAssessmentQuestionsResponseDTO(questions, feedback);
    }

    private Patient getPatient(User user) {
        return patientRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Paciente nao encontrado"));
    }

    private PatientMetrics getMetrics(Patient patient) {
        return metricsRepository.findByPatient(patient)
                .orElseThrow(() -> new RuntimeException("Metricas nao encontradas"));
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

    private DailyAssessmentFeedbackDTO buildFeedback(Patient patient, PatientMetrics metrics) {
        List<ScoreHistory> history = scoreHistoryService.findTop2ByPatient(patient);
        ScoreHistory latest = history.isEmpty() ? null : history.get(0);
        Integer currentScore = latest != null ? latest.getTotalScore() : metrics.getCurrentScore();
        if (currentScore == null) {
            currentScore = 0;
        }
        Integer previousScore = history.size() > 1 ? history.get(1).getTotalScore() : null;

        double variation = scoreCalculationService.calculateVariation(previousScore, currentScore);
        RiskLevel scoreRiskLevel = latest != null && latest.getScoreRiskLevel() != null
                ? latest.getScoreRiskLevel()
                : scoreCalculationService.deriveRiskLevelFromScore(currentScore);

        return new DailyAssessmentFeedbackDTO(currentScore + "/1000", variation, scoreRiskLevel);
    }

    public Page<DailyAssessmentHistoryDTO> getDailyAssessmentHistory(User user, DailyAssessmentFilter filter, Pageable pageable) {
        Patient patient = getPatient(user);
        
        var spec = DailyAssessmentSpecification.withFilters(
            patient,
            filter.getFromDate(),
            filter.getToDate()
        );
        
        return dailyAssessmentRepository.findAll(spec, pageable)
            .map(assessment -> mapToHistoryDTO(assessment, patient));
    }

    private DailyAssessmentHistoryDTO mapToHistoryDTO(DailyAssessment assessment, Patient patient) {
        List<AssessmentAnswerDTO> answers = answerRepository.findByDailyAssessment(assessment).stream()
            .map(answer -> new AssessmentAnswerDTO(
                answer.getQuestion().getId(),
                answer.getQuestion().getTitle(),
                answer.getOption().getId(),
                answer.getOption().getLabel(),
                answer.getOption().getScoreValue()
            ))
            .toList();

        // Build feedback for this specific assessment date
        DailyAssessmentFeedbackDTO feedback = buildFeedbackForDate(patient, assessment);

        return new DailyAssessmentHistoryDTO(
            assessment.getId(),
            assessment.getAssessmentDate(),
            answers,
            feedback
        );
    }

    private DailyAssessmentFeedbackDTO buildFeedbackForDate(Patient patient, DailyAssessment assessment) {
        // Get the score history up to this assessment date
        List<ScoreHistory> history = scoreHistoryService.findByPatientBeforeDate(
            patient, 
            assessment.getCreatedAt()
        );
        
        ScoreHistory latest = history.isEmpty() ? null : history.get(0);
        Integer currentScore = latest != null ? latest.getTotalScore() : 0;
        Integer previousScore = history.size() > 1 ? history.get(1).getTotalScore() : null;

        double variation = scoreCalculationService.calculateVariation(previousScore, currentScore);
        RiskLevel scoreRiskLevel = latest != null && latest.getScoreRiskLevel() != null
                ? latest.getScoreRiskLevel()
                : scoreCalculationService.deriveRiskLevelFromScore(currentScore);

        return new DailyAssessmentFeedbackDTO(currentScore + "/1000", variation, scoreRiskLevel);
    }
}
