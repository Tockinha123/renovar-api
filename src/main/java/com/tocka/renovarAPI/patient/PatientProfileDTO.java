package com.tocka.renovarAPI.patient;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PatientProfileDTO(
    String name,
    LocalDate birthDate,
    String email,
    BigDecimal financialBaseline,
    Integer sessionTimeBaseline
) {
    public PatientProfileDTO(Patient patient) {
        this(
            patient.getName(),
            patient.getBirthDate(),
            patient.getUser().getEmail(),
            patient.getFinancialBaseline(),
            patient.getSessionTimeBaseline()
        );
    }
}