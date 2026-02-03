package com.tocka.renovarAPI.assessment.service;

public record ScoreCalculationResult(
    int totalScore,
    int p1Score,
    int p2Score,
    int p3Score,
    int p4Score,
    int p5Score,
    int p6Score
) {
    public static ScoreCalculationResult fromPillars(
            int p1Score,
            int p2Score,
            int p3Score,
            int p4Score,
            int p5Score,
            int p6Score) {
        int total = p1Score + p2Score + p3Score + p4Score + p5Score + p6Score;
        return new ScoreCalculationResult(total, p1Score, p2Score, p3Score, p4Score, p5Score, p6Score);
    }
}
