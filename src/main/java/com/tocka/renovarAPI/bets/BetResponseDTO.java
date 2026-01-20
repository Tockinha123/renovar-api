package com.tocka.renovarAPI.bets;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BetResponseDTO(
    UUID id,
    BigDecimal amount,
    boolean won,
    SessionTime sessionTime,
    BetCategory category,
    LocalDateTime date
) {
    public BetResponseDTO(Bet bet) {
        this(
            bet.getId(),
            bet.getAmount(),
            bet.isWon(),
            bet.getSessionTime(),
            bet.getCategory(),
            bet.getCreatedAt()
        );
    }
}