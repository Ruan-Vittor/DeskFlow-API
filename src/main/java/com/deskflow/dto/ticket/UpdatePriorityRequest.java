package com.deskflow.dto.ticket;

import com.deskflow.entity.TicketPriority;
import jakarta.validation.constraints.NotNull;

public record UpdatePriorityRequest(
        @NotNull(message = "A prioridade é obrigatória")
        TicketPriority priority
) {
}
