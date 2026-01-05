package com.tocka.renovarAPI.user;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterPatientDTO(
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 255, message = "Quantidade de caracteres do email excede o limite máximo")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 128, message = "Senha deve ser igual ou maior que 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#+\\-_])[A-Za-z\\d@$!%*?&#+\\-_]{8,}$",
        message = "Senha deve conter pelo menos: 1 letra minúscula, 1 letra maiúscula, 1 número e 1 caractere especial"
    )
    String password,

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name,

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve ser no passado")
    LocalDate birthDate,

    @NotNull(message = "Baseline financeiro é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Baseline financeiro deve ser maior que zero")
    BigDecimal financialBaseline,

    @NotNull(message = "Tempo de sessão é obrigatório")
    @Min(value = 1, message = "Tempo de sessão deve ser pelo menos 1 minuto")
    Integer sessionTimeBaseline
) {}