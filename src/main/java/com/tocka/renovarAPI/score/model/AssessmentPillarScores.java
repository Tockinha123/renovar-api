package com.tocka.renovarAPI.score.model;

/**
 * Record holding assessment-related pillar scores (p4, p5, p6).
 * 
 * P4: Craving control - based on daily craving level (0-10)
 * P5: Fixed score for daily check-in completion
 * P6: Reserved for future use
 */
public record AssessmentPillarScores(int p4Score, int p5Score, int p6Score) {
    
    /**
     * Calculate the sum of all assessment pillar scores.
     */
    public int total() {
        return p4Score + p5Score + p6Score;
    }
}
