package com.tocka.renovarAPI.bets;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BetRepository extends JpaRepository<Bet, UUID> {
    // Aqui poderemos criar métodos como findByPatientId futuramente
}