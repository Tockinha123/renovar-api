package com.tocka.renovarAPI.assessment.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO para resposta de uma questão respondida na avaliação")
public record AssessmentAnswerDTO(
    @Schema(description = "ID da pergunta", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID questionId,
    
    @Schema(description = "Texto da pergunta", example = "Como você está se sentindo hoje?")
    String questionText,
    
    @Schema(description = "ID da opção selecionada", example = "550e8400-e29b-41d4-a716-446655440001")
    UUID optionId,
    
    @Schema(description = "Texto da opção selecionada", example = "Bem")
    String optionLabel,
    
    @Schema(description = "Valor da pontuação da opção", example = "5")
    Integer scoreValue
) {}
