package com.deskflow.service.support;

import com.deskflow.exception.InvalidStatusTransitionException;
import com.deskflow.entity.TicketStatus;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TicketStatusTransitionPolicy {

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = Map.of(
            TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED),
            TicketStatus.IN_PROGRESS, EnumSet.of(
                    TicketStatus.WAITING_FOR_USER,
                    TicketStatus.RESOLVED,
                    TicketStatus.CLOSED
            ),
            TicketStatus.WAITING_FOR_USER, EnumSet.of(
                    TicketStatus.IN_PROGRESS,
                    TicketStatus.RESOLVED,
                    TicketStatus.CLOSED
            ),
            TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED, TicketStatus.OPEN),
            TicketStatus.CLOSED, EnumSet.of(TicketStatus.OPEN)
    );

    private static final Map<TicketStatus, Set<TicketStatus>> REQUESTER_TRANSITIONS = Map.of(
            TicketStatus.OPEN, EnumSet.of(TicketStatus.CLOSED),
            TicketStatus.IN_PROGRESS, EnumSet.noneOf(TicketStatus.class),
            TicketStatus.WAITING_FOR_USER, EnumSet.of(TicketStatus.IN_PROGRESS),
            TicketStatus.RESOLVED, EnumSet.of(TicketStatus.OPEN),
            TicketStatus.CLOSED, EnumSet.of(TicketStatus.OPEN)
    );

    public void assertValidTransition(TicketStatus current, TicketStatus target) {
        if (current == null || target == null || !TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }
    }

    public boolean isAllowedForRequester(TicketStatus current, TicketStatus target) {
        return REQUESTER_TRANSITIONS.getOrDefault(current, Set.of()).contains(target);
    }
}
