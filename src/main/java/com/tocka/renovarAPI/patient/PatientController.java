package com.tocka.renovarAPI.patient;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tocka.renovarAPI.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/me")
@Tag(
    name = "Perfil do Paciente"
)
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(
        summary = "Obter Perfil do Paciente",
        description = "Recupera as informações do perfil do paciente autenticado incluindo dados pessoais e baselines."
    )
    public ResponseEntity<PatientProfileDTO> getProfile(@AuthenticationPrincipal User user) {
        PatientProfileDTO profile = patientService.getPatientProfile(user);
        return ResponseEntity.status(HttpStatus.OK).body(profile);
    }
}