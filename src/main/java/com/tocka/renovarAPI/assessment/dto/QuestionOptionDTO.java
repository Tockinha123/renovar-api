package com.tocka.renovarAPI.assessment.dto;

import java.util.UUID;

public record QuestionOptionDTO(
    UUID id,
    String label,
    Integer scoreValue
) {}
