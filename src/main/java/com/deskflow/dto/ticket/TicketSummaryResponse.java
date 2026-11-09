package com.deskflow.dto.ticket;

import com.deskflow.dto.user.UserSummaryResponse;
import com.deskflow.entity.TicketCategory;
import com.deskflow.entity.TicketPriority;
import com.deskflow.entity.TicketStatus;
import java.time.Instant;

public record TicketSummaryResponse(
        Long id,
        String title,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        UserSummaryResponse requester,
        UserSummaryResponse technician,
        Instant createdAt,
        Instant updatedAt
) {
}
