package com.tocka.renovarAPI.assessment;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tocka.renovarAPI.assessment.dto.DailyAssessmentQuestionsResponseDTO;
import com.tocka.renovarAPI.assessment.dto.MonthlyAssessmentQuestionsResponseDTO;
import com.tocka.renovarAPI.assessment.dto.SubmitAssessmentRequestDTO;
import com.tocka.renovarAPI.assessment.service.DailyAssessmentService;
import com.tocka.renovarAPI.assessment.service.MonthlyAssessmentService;
import com.tocka.renovarAPI.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/assessments")
@Tag(name = "Avaliações")
public class AssessmentController {

    private final DailyAssessmentService dailyAssessmentService;
    private final MonthlyAssessmentService monthlyAssessmentService;

    public AssessmentController(DailyAssessmentService dailyAssessmentService,
            MonthlyAssessmentService monthlyAssessmentService) {
        this.dailyAssessmentService = dailyAssessmentService;
        this.monthlyAssessmentService = monthlyAssessmentService;
    }

    @GetMapping("/daily")
    @Operation(summary = "Questionário diário de fissura", description = "Retorna as perguntas diárias e o feedback se já foi respondido hoje. Quando não há resposta no dia, o campo `feedback` é null.")
    public ResponseEntity<DailyAssessmentQuestionsResponseDTO> getDailyAssessment(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dailyAssessmentService.getQuestionsWithStatus(user));
    }

    @PostMapping("/daily")
    @Operation(summary = "Enviar avaliação diária", description = "Registra a avaliação diária e retorna o feedback calculado.")
    public ResponseEntity<DailyAssessmentQuestionsResponseDTO> submitDailyAssessment(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid SubmitAssessmentRequestDTO request) {
        return ResponseEntity.ok(dailyAssessmentService.submitAssessment(user, request));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Questionário mensal PGSI", description = "Retorna as perguntas mensais e o feedback se já foi respondido neste mês. Quando não há resposta no mês, o campo `feedback` é null.")
    public ResponseEntity<MonthlyAssessmentQuestionsResponseDTO> getMonthlyAssessment(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(monthlyAssessmentService.getQuestionsWithStatus(user));
    }

    @PostMapping("/monthly")
    @Operation(summary = "Enviar avaliação mensal", description = "Registra a avaliação mensal PGSI e retorna o feedback calculado.")
    public ResponseEntity<MonthlyAssessmentQuestionsResponseDTO> submitMonthlyAssessment(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid SubmitAssessmentRequestDTO request) {
        return ResponseEntity.ok(monthlyAssessmentService.submitAssessment(user, request));
    }
}