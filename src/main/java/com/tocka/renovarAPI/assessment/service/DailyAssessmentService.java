package com.tocka.renovarAPI.assessment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tocka.renovarAPI.assessment.dto.*;
import com.tocka.renovarAPI.assessment.entities.AssessmentAnswer;
import com.tocka.renovarAPI.assessment.entities.AssessmentOption;
import com.tocka.renovarAPI.assessment.entities.AssessmentQuestion;
import com.tocka.renovarAPI.assessment.entities.AssessmentType;
import com.tocka.renovarAPI.assessment.entities.DailyAssessment;
import com.tocka.renovarAPI.assessment.filter.DailyAssessmentFilter;
import com.tocka.renovarAPI.assessment.repository.*;
import com.tocka.renovarAPI.assessment.specification.DailyAssessmentSpecification;
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
import com.tocka.renovarAPI.score.model.AllPillarScores;
import com.tocka.renovarAPI.user.User;

/**
 * Service da Avaliação Diária.
 * 
 * =====================================================================
 * MUDANÇAS (v2):
 * =====================================================================
 * 
 * 1. Recalcula TODOS os 6 pilares (não só P4-P6).
 *    → P3 agora recupera com o tempo quando chamado pela avaliação diária.
 *    → P5 usa janela de 7 dias ao invés de ser fixo em 80.
 * 
 * 2. Extrai craving APENAS da pergunta de fissura (não da última pergunta do loop).
 * 
 * 3. Usa o valor RAW da label (0-10), não o score_value invertido do banco.
 * =====================================================================
 */
@Service
public class DailyAssessmentService {

    // Keywords pra identificar a pergunta de fissura pelo título
    private static final String FISSURA_KEYWORD_1 = "intensidade da vontade";
    private static final String FISSURA_KEYWORD_2 = "fissura";
    private static final String FISSURA_KEYWORD_3 = "nível de fissura";

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

        // ===================================================================
        // 1. Processar respostas e extrair craving da pergunta CORRETA
        // ===================================================================
        Integer rawCravingLevel = null;
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

            // FIX: Extrair craving APENAS da pergunta de fissura
            if (isCravingQuestion(question)) {
                rawCravingLevel = extractRawCravingFromOption(option);
            }

            AssessmentAnswer answer = new AssessmentAnswer();
            answer.setQuestion(question);
            answer.setOption(option);
            answersToSave.add(answer);
        }

        // Fallback: se não encontrou pergunta de fissura, valor neutro
        if (rawCravingLevel == null) {
            rawCravingLevel = 5;
        }

        // ===================================================================
        // 2. Salvar a avaliação e respostas
        // ===================================================================
        DailyAssessment assessment = new DailyAssessment();
        assessment.setPatient(patient);
        assessment.setAssessmentDate(today);
        assessment.setCravingLevel(rawCravingLevel);
        DailyAssessment savedAssessment = dailyAssessmentRepository.save(assessment);

        for (AssessmentAnswer answer : answersToSave) {
            answer.setDailyAssessment(savedAssessment);
        }
        answerRepository.saveAll(answersToSave);

        // ===================================================================
        // 3. Calcular TODOS os 6 pilares (MUDANÇA v2)
        // ===================================================================
        AllPillarScores scores = scoreCalculationService
                .calculateAllForDailyAssessment(patient, metrics, rawCravingLevel);

        // ===================================================================
        // 4. Criar registro no histórico de score
        // ===================================================================
        ScoreHistory history = scoreHistoryService
                .createScoreHistoryForDailyAssessment(patient, scores, savedAssessment.getId());

        // ===================================================================
        // 5. Atualizar métricas
        // ===================================================================
        metrics.setCurrentScore(history.getTotalScore());
        metrics.setCurrentRiskLevel(history.getScoreRiskLevel());
        metrics.setLastCheckin(LocalDateTime.now());
        metricsRepository.save(metrics);

        DailyAssessmentFeedbackDTO feedback = buildFeedback(patient, metrics);
        List<QuestionDTO> questions = buildQuestions(AssessmentType.DAILY);
        return new DailyAssessmentQuestionsResponseDTO(questions, feedback);
    }

    // =====================================================================
    // EXTRAÇÃO DE CRAVING
    // =====================================================================

    /**
     * Identifica se a pergunta é sobre fissura/craving pelo título.
     */
    private boolean isCravingQuestion(AssessmentQuestion question) {
        String title = question.getTitle().toLowerCase();
        return title.contains(FISSURA_KEYWORD_1)
                || title.contains(FISSURA_KEYWORD_2)
                || title.contains(FISSURA_KEYWORD_3);
    }

    /**
     * Extrai o valor RAW de craving (0-10) da opção selecionada.
     * 
     * O banco armazena score_value INVERTIDO (label "0" → score_value 10).
     * A fórmula P4 também inverte (120 - craving*12).
     * Pra evitar dupla inversão, extraímos o valor da LABEL.
     */
    private int extractRawCravingFromOption(AssessmentOption option) {
        // Tenta extrair valor numérico da label (ex: "0", "1", ..., "10")
        try {
            int labelValue = Integer.parseInt(option.getLabel().trim());
            if (labelValue >= 0 && labelValue <= 10) {
                return labelValue;
            }
        } catch (NumberFormatException ignored) {
            // Label não numérica (ex: "Nenhuma Vontade", "Vontade Leve")
        }

        // Fallback: desinverter o score_value
        // score_value: 10 = sem fissura, 0 = fissura máxima
        // raw craving: 0 = sem fissura, 10 = fissura máxima
        return 10 - option.getScoreValue();
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

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
                .sorted(Comparator.comparing(AssessmentQuestion::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(question -> {
                    List<QuestionOptionDTO> options = optionRepository
                            .findByQuestionAndActiveTrue(question).stream()
                            .sorted(Comparator.comparing(AssessmentOption::getScoreValue))
                            .map(option -> new QuestionOptionDTO(
                                    option.getId(), option.getLabel(), option.getScoreValue()))
                            .toList();
                    return new QuestionDTO(question.getId(), question.getTitle(), options);
                })
                .toList();
    }

    private DailyAssessmentFeedbackDTO buildFeedback(Patient patient, PatientMetrics metrics) {
        List<ScoreHistory> history = scoreHistoryService.findTop2ByPatient(patient);
        ScoreHistory latest = history.isEmpty() ? null : history.get(0);
        Integer currentScore = latest != null ? latest.getTotalScore() : metrics.getCurrentScore();
        if (currentScore == null) currentScore = 0;
        Integer previousScore = history.size() > 1 ? history.get(1).getTotalScore() : null;

        double variation = scoreCalculationService.calculateVariation(previousScore, currentScore);
        RiskLevel scoreRiskLevel = latest != null && latest.getScoreRiskLevel() != null
                ? latest.getScoreRiskLevel()
                : scoreCalculationService.deriveRiskLevelFromScore(currentScore);

        return new DailyAssessmentFeedbackDTO(currentScore + "/1000", variation, scoreRiskLevel);
    }

    // =====================================================================
    // HISTÓRICO
    // =====================================================================

    public Page<DailyAssessmentHistoryDTO> getDailyAssessmentHistory(
            User user, DailyAssessmentFilter filter, Pageable pageable) {
        Patient patient = getPatient(user);
        var spec = DailyAssessmentSpecification.withFilters(
                patient, filter.getFromDate(), filter.getToDate());
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
                        answer.getOption().getScoreValue()))
                .toList();

        DailyAssessmentFeedbackDTO feedback = buildFeedbackForDate(patient, assessment);
        return new DailyAssessmentHistoryDTO(
                assessment.getId(), assessment.getAssessmentDate(), answers, feedback);
    }

    private DailyAssessmentFeedbackDTO buildFeedbackForDate(Patient patient, DailyAssessment assessment) {
        List<ScoreHistory> history = scoreHistoryService.findByPatientBeforeDate(
                patient, assessment.getCreatedAt());
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