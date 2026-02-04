package com.tocka.renovarAPI.score.model;

/**
 * Enum representing the source/trigger of a score calculation.
 * Used for audit tracking in ScoreHistory.
 */
public enum CalculationSource {
    BET_OPERATION,
    DAILY_ASSESSMENT,
    MONTHLY_ASSESSMENT,
    MANUAL_RECALCULATION
}
