package com.tocka.renovarAPI.patient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tocka.renovarAPI.user.User;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByUser(User user);
    boolean existsByUser(User user);
}