package com.tocka.renovarAPI.report;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tocka.renovarAPI.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Relatórios Mensais")
public class ReportController {

    private final ReportService reportService;
    
    public ReportController(ReportService reportService, ReportScheduler reportScheduler) {
        this.reportService = reportService;
    }

    @GetMapping
    @Operation(
        summary = "Listar Relatórios",
        description = "Retorna todos os relatórios do paciente ordenados por data (mais recente primeiro)."
    )
    public ResponseEntity<List<ReportResponseDTO>> listReports(@AuthenticationPrincipal User user) {
        List<ReportResponseDTO> reports = reportService.listReports(user);
        return ResponseEntity.ok(reports);
    }

    @PostMapping
    @Operation(
        summary = "Gerar Relatório Mensal",
        description =   "Gera o relatório de um mês/ano específico. Não permite gerar relatórios de meses futuros." + 
                        "Utilizado pelo cron job para gerar relatórios automaticamente no dia 28 de cada mês."
    )
    public ResponseEntity<ReportResponseDTO> generateReport(
            @AuthenticationPrincipal User user,
            @RequestParam int month,
            @RequestParam int year) {
        ReportResponseDTO report = reportService.generateMonthlyReport(user, month, year);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    @PostMapping("/previous-month")
    @Operation(
        summary = "Gerar Relatório do Mês Anterior",
        description = "Gera automaticamente o relatório do mês anterior ao atual. Caso o relatório já exista, retorna um erro"
    )
    public ResponseEntity<ReportResponseDTO> generatePreviousMonthReport(
            @AuthenticationPrincipal User user) {
        ReportResponseDTO report = reportService.generatePreviousMonthReport(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    @GetMapping("/current")
    @Operation(
        summary = "Obter Relatório do Mês Atual",
        description = "Retorna o relatório do mês atual se existir."
    )
    public ResponseEntity<ReportResponseDTO> getCurrentMonthReport(
            @AuthenticationPrincipal User user) {
        ReportResponseDTO report = reportService.getCurrentMonthReport(user);
        if (report == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(report);
    }

    @GetMapping("/download")
    @Operation(
        summary = "Baixar Relatório do Período Atual",
        description = "Identifica automaticamente o relatório do período atual (ciclo dia 28 a dia 27) " +
                      "e retorna a URL de download. Se o relatório ainda não existir, ele é gerado automaticamente. " +
                      "Pacientes que criaram conta após o dia 28 só terão acesso ao relatório do mês seguinte."
    )
    public ResponseEntity<DownloadUrlDTO> downloadCurrentReport(
            @AuthenticationPrincipal User user) throws Exception {
        DownloadUrlDTO url = reportService.getOrGenerateCurrentReport(user);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/{reportId}/download")
    @Operation(
        summary = "Obter URL de Download por ID",
        description = "Retorna uma URL pré-assinada para download do PDF de um relatório específico."
    )
    public ResponseEntity<DownloadUrlDTO> getDownloadUrl(
            @AuthenticationPrincipal User user,
            @PathVariable String reportId) throws Exception {
        String url = reportService.getDownloadUrl(user, reportId);
        return ResponseEntity.ok(new DownloadUrlDTO(url));
    }
}