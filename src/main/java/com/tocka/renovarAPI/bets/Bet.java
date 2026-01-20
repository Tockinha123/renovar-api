package com.tocka.renovarAPI.bets;

import com.tocka.renovarAPI.patient.Patient; 
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relacionamento com Paciente (Muitas apostas para um paciente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private BigDecimal amount; // Valor apostado

    @Column(nullable = false)
    private boolean won; // Checkbox: true = Ganhei, false = Perdi

    @Enumerated(EnumType.STRING)
    @Column(name = "session_time", nullable = false)
    private SessionTime sessionTime; // Enum de tempo (5, 15, 30...)

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private BetCategory category; // O segundo enum (Tipo de jogo)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}