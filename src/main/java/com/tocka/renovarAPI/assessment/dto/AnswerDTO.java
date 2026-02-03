package com.tocka.renovarAPI.assessment.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AnswerDTO(
    @NotNull(message = "Question id is required")
    UUID questionId,
    @NotNull(message = "Option id is required")
    UUID optionId
) {}
