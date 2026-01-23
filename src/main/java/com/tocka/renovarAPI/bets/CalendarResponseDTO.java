package com.tocka.renovarAPI.bets;

import java.time.LocalDate;

public record CalendarResponseDTO(
    LocalDate date,
    DayStatus status
) {
    
}
