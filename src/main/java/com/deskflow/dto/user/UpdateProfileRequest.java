package com.deskflow.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String name,

        @Size(max = 72, message = "A senha atual deve ter no máximo 72 caracteres")
        String currentPassword,

        @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres")
        String newPassword
) {
}
