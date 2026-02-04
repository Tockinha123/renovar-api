package com.tocka.renovarAPI.metrics;

public enum RiskLevel {
    EXCELENTE,   // Score 701-1000 (Verde)
    BOM,         // Score 501-700  (Verde claro)
    REGULAR,     // Score 301-500  (Amarelo - Atenção)
    ALTO_RISCO;  // Score 0-300    (Vermelho)

    public static RiskLevel fromScore(int score) {
        if (score >= 701) {
            return RiskLevel.EXCELENTE;
        }
        if (score >= 501) {
            return RiskLevel.BOM;
        }
        if (score >= 301) {
            return RiskLevel.REGULAR;
        }
        return RiskLevel.ALTO_RISCO;
    }

    public static RiskLevel fromPgsi(int pgsiScore) {
        if (pgsiScore == 0) {
            return RiskLevel.EXCELENTE;
        }
        if (pgsiScore <= 2) {
            return RiskLevel.BOM;
        }
        if (pgsiScore <= 7) {
            return RiskLevel.REGULAR;
        }
        return RiskLevel.ALTO_RISCO;
    }
}
