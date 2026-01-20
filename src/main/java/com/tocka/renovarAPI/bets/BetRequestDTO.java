package com.tocka.renovarAPI.bets;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BetRequestDTO(
    @NotNull(message = "O valor da aposta é obrigatório")
    @Positive(message = "O valor deve ser positivo")
    BigDecimal amount,

    @NotNull(message = "O resultado da aposta é obrigatório")
    Boolean won, // Checkbox marcada = true

    @NotNull(message = "O tempo de sessão é obrigatório")
    SessionTime sessionTime,

    @NotNull(message = "A categoria é obrigatória")
    BetCategory category
) {}