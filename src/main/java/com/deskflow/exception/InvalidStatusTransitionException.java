package com.deskflow.exception;

import com.deskflow.entity.TicketStatus;

public class InvalidStatusTransitionException extends BusinessRuleException {

    private final TicketStatus currentStatus;
    private final TicketStatus targetStatus;

    public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus targetStatus) {
        super("Transição de status inválida: %s -> %s".formatted(currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public TicketStatus getTargetStatus() {
        return targetStatus;
    }
}
