package com.deskflow.dto.ticket;

import com.deskflow.dto.user.UserSummaryResponse;
import com.deskflow.entity.TicketCategory;
import com.deskflow.entity.TicketPriority;
import com.deskflow.entity.TicketStatus;
import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        UserSummaryResponse requester,
        UserSummaryResponse technician,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {
}
