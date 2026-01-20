package com.tocka.renovarAPI.bets;

import com.tocka.renovarAPI.user.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bets") 
public class BetController {

    private final BetService betService;

    public BetController(BetService betService) {
        this.betService = betService;
    }

    @PostMapping
    public ResponseEntity<BetResponseDTO> registrar(@RequestBody @Valid BetRequestDTO data, @AuthenticationPrincipal User user) {
        // ✅ Correção 2: Recebe o DTO criado
        var betResponse = betService.registrarAposta(user, data);
        
        // Retorna 201 Created com o objeto criado. Isso é o padrão REST correto.
        return ResponseEntity.status(HttpStatus.CREATED).body(betResponse);
    }

    @GetMapping
    public ResponseEntity<Page<BetResponseDTO>> listar(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var page = betService.listarApostas(user, pageable);
        return ResponseEntity.ok(page);
    }
}