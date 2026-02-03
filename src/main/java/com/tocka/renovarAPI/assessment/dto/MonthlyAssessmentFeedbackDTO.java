package com.tocka.renovarAPI.assessment.dto;

import com.tocka.renovarAPI.metrics.RiskLevel;

public record MonthlyAssessmentFeedbackDTO(
    String resultPgsi,
    Double variationPgsi,
    RiskLevel riskLevelPgsi
) {}
