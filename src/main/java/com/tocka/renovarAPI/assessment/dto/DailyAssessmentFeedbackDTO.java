package com.tocka.renovarAPI.assessment.dto;

import com.tocka.renovarAPI.metrics.RiskLevel;

public record DailyAssessmentFeedbackDTO(
    String resultScore,
    Double variationScore,
    RiskLevel riskLevelScore
) {}
