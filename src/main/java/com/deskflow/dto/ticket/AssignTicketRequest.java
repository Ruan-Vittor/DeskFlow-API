package com.deskflow.dto.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignTicketRequest(
        @NotNull(message = "O técnico é obrigatório")
        @Positive(message = "O identificador do técnico deve ser positivo")
        Long technicianId
) {
}
