package com.tocka.renovarAPI.metrics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.tocka.renovarAPI.patient.Patient;

/**
 * Serviço responsável pelos cálculos de métricas do Dashboard.
 * Separação de responsabilidade: a entidade não deve conter lógica de negócio.
 */
@Service
public class MetricsCalculatorService {

    /**
     * Calcula dias limpos em tempo real.
     * Marco zero: última aposta OU data de criação do usuário (se nunca apostou).
     */
    public long calcularDiasLimpos(PatientMetrics metrics) {
        LocalDateTime marcoZero = (metrics.getLastBetAt() != null) 
                ? metrics.getLastBetAt() 
                : metrics.getPatient().getCreatedAt();
        
        if (marcoZero == null) {
            return 0;
        }
        
        long dias = ChronoUnit.DAYS.between(marcoZero.toLocalDate(), LocalDateTime.now().toLocalDate());
        return Math.max(dias, 0);
    }

    /**
     * Calcula economia acumulada baseada nos dias limpos * baseline financeiro.
     * Fórmula: E_total = Σ(δ_d × V_base) onde δ_d = 1 para dias limpos
     */
    public BigDecimal calcularEconomia(PatientMetrics metrics) {
        Patient patient = metrics.getPatient();
        
        if (patient.getFinancialBaseline() == null) {
            return BigDecimal.ZERO;
        }
        
        long diasLimpos = calcularDiasLimpos(metrics);
        return patient.getFinancialBaseline().multiply(BigDecimal.valueOf(diasLimpos));
    }

    /**
     * Calcula tempo de vida salvo (em horas).
     * Fórmula: T_salvo = Σ(δ_d × H_base)
     */
    public long calcularHorasSalvas(PatientMetrics metrics) {
        Patient patient = metrics.getPatient();
        
        if (patient.getSessionTimeBaseline() == null || patient.getSessionTimeBaseline() == 0) {
            return 0;
        }
        
        long diasLimpos = calcularDiasLimpos(metrics);
        long minutosTotais = diasLimpos * patient.getSessionTimeBaseline();
        return minutosTotais / 60;
    }

    public RiskLevel calcularRiskLevel(int score) {
        return RiskLevel.fromScore(score);
    }

    /**
     * Atualiza o RiskLevel na entidade de métricas baseado no score atual.
     */
    public void atualizarRiskLevel(PatientMetrics metrics) {
        RiskLevel novoRisco = calcularRiskLevel(metrics.getCurrentScore());
        metrics.setCurrentRiskLevel(novoRisco);
    }

    /**
     * Aplica a lógica de Soft Reset na streak.
     * Fórmula: Streak_nova = floor(Streak_anterior / 2)
     */
    public int aplicarSoftReset(int streakAtual) {
        return streakAtual / 2; // Integer division já faz o floor
    }

    public int calcularNovoScoreAposRecaida(int scoreAtual, BigDecimal valorAposta, BigDecimal baseline) {
        if (baseline == null) baseline = BigDecimal.ZERO;
        
        double fatorPenalidade;
        
        if (valorAposta.compareTo(baseline) > 0) {
            fatorPenalidade = 0.5; 
        } else {

            fatorPenalidade = 0.75; 
        }
        
        return (int) (scoreAtual * fatorPenalidade);
    }
}
