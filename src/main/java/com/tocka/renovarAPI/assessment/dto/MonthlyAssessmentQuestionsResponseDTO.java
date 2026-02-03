package com.tocka.renovarAPI.assessment.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record MonthlyAssessmentQuestionsResponseDTO(
    List<QuestionDTO> questions,
    @Schema(
            nullable = true,
            description = "Null quando não há avaliação mensal no mês."
    )
    MonthlyAssessmentFeedbackDTO feedback
) {}
