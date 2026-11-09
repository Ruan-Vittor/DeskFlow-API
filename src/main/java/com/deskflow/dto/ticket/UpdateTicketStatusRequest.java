package com.deskflow.dto.ticket;

import com.deskflow.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull(message = "O status é obrigatório")
        TicketStatus status
) {
}
