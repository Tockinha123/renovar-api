package com.tocka.renovarAPI.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tocka.renovarAPI.infra.security.TokenService;
import com.tocka.renovarAPI.patient.PatientService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("api/v1/auth")
public class AuthenticationController {

    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final PatientService patientService;

    public AuthenticationController(TokenService tokenService,
                                    AuthenticationManager authenticationManager,
                                    PatientService patientService) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.patientService = patientService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid UserRequest data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> register(@RequestBody @Valid RegisterPatientDTO data) {
        patientService.registerPatient(data);
        return ResponseEntity.status(HttpStatus.CREATED).body("Paciente registrado com sucesso");
    }
}