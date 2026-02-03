package com.tocka.renovarAPI.assessment.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SubmitAssessmentRequestDTO(
    @NotNull(message = "Answers are required")
    @NotEmpty(message = "Answers are required")
    List<AnswerDTO> answers
) {}
