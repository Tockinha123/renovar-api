package com.tocka.renovarAPI.assessment.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record DailyAssessmentQuestionsResponseDTO(
    List<QuestionDTO> questions,
    @Schema(
            nullable = true,
            description = "Null quando não há avaliação diária no dia."
    )
    DailyAssessmentFeedbackDTO feedback
) {}
