package com.tocka.renovarAPI.assessment.validation;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tocka.renovarAPI.assessment.dto.AnswerDTO;
import com.tocka.renovarAPI.assessment.entities.AssessmentQuestion;
import com.tocka.renovarAPI.assessment.entities.AssessmentType;
import com.tocka.renovarAPI.assessment.repository.AssessmentQuestionRepository;
import com.tocka.renovarAPI.infra.exception.AssessmentAnswerValidationError;
import com.tocka.renovarAPI.infra.exception.InvalidAssessmentSubmissionException;

@Component
public class AssessmentSubmissionValidator {

    private final AssessmentQuestionRepository questionRepository;

    public AssessmentSubmissionValidator(AssessmentQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public void validateAnswers(AssessmentType type, List<AnswerDTO> answers) {
        List<AssessmentQuestion> activeQuestions = questionRepository.findByTypeAndActiveTrue(type);
        Set<UUID> expectedQuestionIds = activeQuestions.stream()
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UUID> submittedQuestionIds = answers.stream()
                .map(AnswerDTO::questionId)
                .toList();

        Set<UUID> uniqueSubmitted = new LinkedHashSet<>(submittedQuestionIds);

        Map<UUID, Long> counts = submittedQuestionIds.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Set<UUID> duplicateQuestionIds = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<UUID> missingQuestionIds = new LinkedHashSet<>(expectedQuestionIds);
        missingQuestionIds.removeAll(uniqueSubmitted);

        Set<UUID> extraQuestionIds = new LinkedHashSet<>(uniqueSubmitted);
        extraQuestionIds.removeAll(expectedQuestionIds);

        EnumSet<AssessmentAnswerValidationError> errors = EnumSet.noneOf(AssessmentAnswerValidationError.class);
        if (!missingQuestionIds.isEmpty()) {
            errors.add(AssessmentAnswerValidationError.MISSING_ANSWER);
        }
        if (!extraQuestionIds.isEmpty()) {
            errors.add(AssessmentAnswerValidationError.EXTRA_ANSWER);
        }
        if (!duplicateQuestionIds.isEmpty()) {
            errors.add(AssessmentAnswerValidationError.DUPLICATE_ANSWER);
        }

        if (!errors.isEmpty()) {
            throw new InvalidAssessmentSubmissionException(
                    "Respostas inválidas para avaliação",
                    errors,
                    missingQuestionIds,
                    extraQuestionIds,
                    duplicateQuestionIds);
        }
    }
}
