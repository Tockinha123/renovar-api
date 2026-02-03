package com.tocka.renovarAPI.infra.exception;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class InvalidAssessmentSubmissionException extends RuntimeException {

    private final EnumSet<AssessmentAnswerValidationError> errorTypes;
    private final List<UUID> missingQuestionIds;
    private final List<UUID> extraQuestionIds;
    private final List<UUID> duplicateQuestionIds;

    public InvalidAssessmentSubmissionException(
            String message,
            EnumSet<AssessmentAnswerValidationError> errorTypes,
            Set<UUID> missingQuestionIds,
            Set<UUID> extraQuestionIds,
            Set<UUID> duplicateQuestionIds) {
        super(message);
        this.errorTypes = errorTypes == null
                ? EnumSet.noneOf(AssessmentAnswerValidationError.class)
                : EnumSet.copyOf(errorTypes);
        this.missingQuestionIds = List.copyOf(missingQuestionIds == null ? Set.of() : missingQuestionIds);
        this.extraQuestionIds = List.copyOf(extraQuestionIds == null ? Set.of() : extraQuestionIds);
        this.duplicateQuestionIds = List.copyOf(duplicateQuestionIds == null ? Set.of() : duplicateQuestionIds);
    }

    public EnumSet<AssessmentAnswerValidationError> getErrorTypes() {
        return EnumSet.copyOf(errorTypes);
    }

    public List<UUID> getMissingQuestionIds() {
        return missingQuestionIds;
    }

    public List<UUID> getExtraQuestionIds() {
        return extraQuestionIds;
    }

    public List<UUID> getDuplicateQuestionIds() {
        return duplicateQuestionIds;
    }
}
