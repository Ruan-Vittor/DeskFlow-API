package com.deskflow.dto.ticket;

import com.deskflow.entity.TicketCategory;
import com.deskflow.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String title,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(min = 10, max = 4000, message = "A descrição deve ter entre 10 e 4000 caracteres")
        String description,

        @NotNull(message = "A categoria é obrigatória")
        TicketCategory category,

        @NotNull(message = "A prioridade é obrigatória")
        TicketPriority priority
) {
}
