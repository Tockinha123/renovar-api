package com.tocka.renovarAPI.bets;

public enum SessionTime {
    FIVE_MINUTES("5 minutos"),
    FIFTEEN_MINUTES("15 minutos"),
    THIRTY_MINUTES("30 minutos"),
    SIXTY_PLUS_MINUTES("60+ minutos");

    private final String description;

    SessionTime(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}