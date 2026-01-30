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
    int scoreInicio,
    int scoreFim,
    int scoreMedio,
    String fraseMotivar
) {}