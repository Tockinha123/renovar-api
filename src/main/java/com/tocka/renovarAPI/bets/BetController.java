package com.tocka.renovarAPI.bets;

import com.tocka.renovarAPI.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1/bets")
@Tag(
    name = "Apostas"
)
public class BetController {

    private final BetService betService;

    public BetController(BetService betService) {
        this.betService = betService;
    }

    @PostMapping
    @Operation(
        summary = "Cadastro de apostas.",
        description = "Registra uma nova aposta no sistema."
    )
    public ResponseEntity<BetResponseDTO> registrar(@RequestBody @Valid BetRequestDTO data, @AuthenticationPrincipal User user) {
        // ✅ Correção 2: Recebe o DTO criado
        var betResponse = betService.registrarAposta(user, data);
        
        // Retorna 201 Created com o objeto criado. Isso é o padrão REST correto.
        return ResponseEntity.status(HttpStatus.CREATED).body(betResponse);
    }

    @GetMapping
    @Operation(
        summary = "Lista as apostas do usuário",
        description = "Recupera as apostas de um usuário permitindo a visualização de uma lista paginada de suas apostas."
    )
    public ResponseEntity<Page<BetResponseDTO>> listar(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var page = betService.listarApostas(user, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/calendar")
    @Operation(
        summary = "Listar os dias do mês com apostas",
        description = "Gera uma lista dos dias do mês de um determinado ano indicando quais dias o paciente fez apostas."
    )
    public ResponseEntity<List<CalendarResponseDTO>> getMethodName(
            @AuthenticationPrincipal User user,
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<CalendarResponseDTO> calendario = betService.gerarCalendario(user, month, year);
        return ResponseEntity.status(HttpStatus.OK).body(calendario);
    }
    
}