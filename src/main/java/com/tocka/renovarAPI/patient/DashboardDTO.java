package com.tocka.renovarAPI.patient;

import java.math.BigDecimal;

import com.tocka.renovarAPI.metrics.RiskLevel;

/**
 * DTO para a tela de Dashboard do paciente.
 * Contém todas as métricas calculadas em tempo real.
 */
public record DashboardDTO(
    Integer currentScore,
    RiskLevel riskLevel,
    long diasLimpos,
    BigDecimal economiaAcumulada,
    long horasSalvas,
    Integer cleanDaysStreak
) {}
