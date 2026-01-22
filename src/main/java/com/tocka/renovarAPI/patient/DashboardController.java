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
@RequestMapping("/api/v1/dashboard")
@Tag(
    name = "Dashboard do Paciente"
)
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(
        summary = "Obter Informações do Dashboard",
        description = "Recupera as informações principais do paciente para a visualização no dashboard."
    )
    public ResponseEntity<DashboardDTO> getDashboard(@AuthenticationPrincipal User user) {
        DashboardDTO dashboardInformations = dashboardService.gerarDashboard(user);
        return ResponseEntity.status(HttpStatus.OK).body(dashboardInformations);
    }
}
