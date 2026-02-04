package com.tocka.renovarAPI.assessment.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO para resposta de histórico de avaliações diárias")
public record DailyAssessmentHistoryDTO(
    @Schema(description = "ID da avaliação", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Data da avaliação", example = "2024-01-15")
    LocalDate assessmentDate,
    
    @Schema(description = "Lista de respostas")
    List<AssessmentAnswerDTO> answers,
    
    @Schema(description = "Feedback do dia")
    DailyAssessmentFeedbackDTO feedback
) {}
