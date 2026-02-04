package com.tocka.renovarAPI.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tocka.renovarAPI.infra.security.TokenService;
import com.tocka.renovarAPI.patient.PatientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
    name = "Autenticação e Registro de Pacientes"
)
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
    @Operation(
        summary = "Login do Paciente",
        description = "Autentica um paciente possibilitando o acesso ao sistema."
    )
    @SecurityRequirements(value = {})
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid UserRequest data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        var user = (User) auth.getPrincipal();
        
        var token = tokenService.generateToken(user);
        var patientName = patientService.getPatientNameByUser(user);
        
        var loginResponse = new LoginResponseDTO(patientName, user.getEmail(), token);
        
        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }

    @PostMapping("/register")
    @Operation(
        summary = "Cadastro do Paciente",
        description = "Registra um novo paciente no sistema e retorna os dados para login automático."
    )
    @SecurityRequirements(value = {})
    public ResponseEntity<LoginResponseDTO> register(@RequestBody @Valid RegisterPatientDTO data) {
        LoginResponseDTO loginResponse = patientService.registerPatient(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(loginResponse);
    }
}
