package com.deskflow.mapper;

import com.deskflow.dto.ticket.TicketResponse;
import com.deskflow.dto.ticket.TicketSummaryResponse;
import com.deskflow.entity.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    private final UserMapper userMapper;

    public TicketMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                userMapper.toSummary(ticket.getRequester()),
                userMapper.toSummary(ticket.getTechnician()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getClosedAt()
        );
    }

    public TicketSummaryResponse toSummary(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                userMapper.toSummary(ticket.getRequester()),
                userMapper.toSummary(ticket.getTechnician()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
