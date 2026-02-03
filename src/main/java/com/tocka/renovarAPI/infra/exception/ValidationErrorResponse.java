package com.tocka.renovarAPI.infra.exception;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidationErrorResponse")
public record ValidationErrorResponse(
        @Schema(example = "2026-01-31T10:15:30") LocalDateTime timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Erro de Validação") String error,
        Map<String, String> errors,
        @Schema(example = "/api/v1/assessments/daily") String path) {
}
