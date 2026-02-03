package com.tocka.renovarAPI.assessment.dto;

import java.util.List;
import java.util.UUID;

public record QuestionDTO(
    UUID id,
    String title,
    List<QuestionOptionDTO> options
) {}
