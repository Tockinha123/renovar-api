package com.tocka.renovarAPI.score.model;

/**
 * Record holding bet-related pillar scores (p1, p2, p3).
 * 
 * P1: Clean days ratio - based on days without bet in last 30 days
 * P2: Streak - based on current clean days streak
 * P3: Financial - based on last bet amount vs baseline
 */
public record BetPillarScores(int p1Score, int p2Score, int p3Score) {
    
    /**
     * Calculate the sum of all bet pillar scores.
     */
    public int total() {
        return p1Score + p2Score + p3Score;
    }
}
