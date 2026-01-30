package com.tocka.renovarAPI.report;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportResponseDTO(
    UUID id,
    String fileName,
    Integer referenceMonth,
    Integer referenceYear,
    ReportStatus status,
    LocalDateTime createdAt,
    boolean available // true se já pode ser baixado (mês seguinte)
) {
    public ReportResponseDTO(Report report, boolean available) {
        this(
            report.getId(),
            report.getFileName(),
            report.getReferenceMonth(),
            report.getReferenceYear(),
            report.getStatus(),
            report.getCreatedAt(),
            available
        );
    }
}