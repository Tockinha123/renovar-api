package com.tocka.renovarAPI.report;

import java.math.BigDecimal;

public record MonthlyReportDataDTO(
    String patientName,
    int referenceMonth,
    int referenceYear,
    int maiorStreak,
    long horasSalvas,
    BigDecimal dinheiroEconomizado,
    int quantidadeApostas,
    double scoreMedio,
    String fraseMotivar
) {}