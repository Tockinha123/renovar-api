package com.tocka.renovarAPI.bets;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tocka.renovarAPI.patient.Patient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BetRepository extends JpaRepository<Bet, UUID> {
    // Aqui poderemos criar métodos como findByPatientId futuramente
    Page<Bet> findByPatientOrderByCreatedAtDesc(Patient patient, Pageable pageable);

    List<Bet> findByPatientAndCreatedAtBetween(Patient patient, LocalDateTime start, LocalDateTime end);

    Bet findTopByPatientOrderByCreatedAtDesc(Patient patient);
}
