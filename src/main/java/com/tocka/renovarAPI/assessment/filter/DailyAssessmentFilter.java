package com.tocka.renovarAPI.assessment.filter;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filtros para consulta de histórico de avaliações diárias")
public class DailyAssessmentFilter {
    
    @Schema(description = "Data inicial do período (inclusive)", example = "2024-01-01")
    private LocalDate fromDate;
    
    @Schema(description = "Data final do período (inclusive)", example = "2024-01-31")
    private LocalDate toDate;

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }
}
