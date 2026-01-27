package com.tocka.renovarAPI.user;

public record LoginResponseDTO(
    String name,
    String email,
    String token
) {
}