package com.tocka.renovarAPI.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 255, message = "Quantidade de caracteres do email excede o limite máximo")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 128, message = "Senha deve ser igual ou maior que 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#+\\-_])[A-Za-z\\d@$!%*?&#+\\-_]{8,}$",
        message = "Senha deve conter pelo menos: 1 letra minúscula, 1 letra maiúscula, 1 número e 1 caractere especial (@$!%*?&#+\\-_)"
    )
    String password
) {
}