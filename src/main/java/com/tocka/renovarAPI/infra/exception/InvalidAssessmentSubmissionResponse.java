package com.tocka.renovarAPI.infra.exception;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InvalidAssessmentSubmissionResponse")
public record InvalidAssessmentSubmissionResponse(
        @Schema(example = "2026-01-31T10:15:30") LocalDateTime timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Avaliação Inválida") String error,
        @Schema(example = "Respostas inválidas para avaliação") String message,
        List<String> types,
        List<String> missingQuestionIds,
        List<String> extraQuestionIds,
        List<String> duplicateQuestionIds,
        @Schema(example = "/api/v1/assessments/daily") String path) {
}
