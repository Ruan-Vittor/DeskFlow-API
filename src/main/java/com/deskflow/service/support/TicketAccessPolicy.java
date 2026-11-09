package com.deskflow.service.support;

import com.deskflow.entity.Role;
import com.deskflow.entity.Ticket;
import com.deskflow.entity.TicketStatus;
import com.deskflow.exception.BusinessRuleException;
import com.deskflow.exception.ForbiddenOperationException;
import com.deskflow.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
public class TicketAccessPolicy {

    private final TicketStatusTransitionPolicy transitionPolicy;

    public TicketAccessPolicy(TicketStatusTransitionPolicy transitionPolicy) {
        this.transitionPolicy = transitionPolicy;
    }

    public boolean canView(Ticket ticket, UserPrincipal principal) {
        if (principal.role() == Role.ADMIN) {
            return true;
        }
        if (isOwner(ticket, principal)) {
            return true;
        }
        if (principal.role() == Role.TECHNICIAN) {
            return ticket.getTechnician() == null || ticket.isAssignedTo(principal.id());
        }
        return false;
    }

    public void assertCanView(Ticket ticket, UserPrincipal principal) {
        if (!canView(ticket, principal)) {
            throw new ForbiddenOperationException("Você não tem permissão para acessar este chamado.");
        }
    }

    public void assertCanEdit(Ticket ticket, UserPrincipal principal) {
        if (ticket.isClosed()) {
            throw new BusinessRuleException("Um chamado encerrado não pode ser editado.");
        }
        boolean allowed = principal.role() == Role.ADMIN
                || (principal.role() == Role.REQUESTER && isOwner(ticket, principal));
        if (!allowed) {
            throw new ForbiddenOperationException("Você não tem permissão para editar este chamado.");
        }
    }

    public void assertCanComment(Ticket ticket, UserPrincipal principal) {
        assertCanView(ticket, principal);
        if (ticket.isClosed()) {
            throw new BusinessRuleException("Um chamado encerrado não pode receber novos comentários.");
        }
    }

    public void assertCanAssign(Ticket ticket, UserPrincipal principal) {
        if (ticket.isClosed()) {
            throw new BusinessRuleException("Um chamado encerrado não pode ser atribuído.");
        }
        if (principal.role() != Role.ADMIN && principal.role() != Role.TECHNICIAN) {
            throw new ForbiddenOperationException("Apenas técnicos e administradores podem atribuir chamados.");
        }
    }

    public void assertCanChangePriority(Ticket ticket, UserPrincipal principal) {
        if (ticket.isClosed()) {
            throw new BusinessRuleException("Um chamado encerrado não pode ter a prioridade alterada.");
        }
        boolean allowed = principal.role() == Role.ADMIN
                || (principal.role() == Role.TECHNICIAN && ticket.isAssignedTo(principal.id()));
        if (!allowed) {
            throw new ForbiddenOperationException("Você não tem permissão para alterar a prioridade deste chamado.");
        }
    }

    public void assertCanChangeStatus(Ticket ticket, TicketStatus target, UserPrincipal principal) {
        if (principal.role() == Role.ADMIN) {
            return;
        }
        if (principal.role() == Role.TECHNICIAN && ticket.isAssignedTo(principal.id())) {
            return;
        }
        if (principal.role() == Role.REQUESTER
                && isOwner(ticket, principal)
                && transitionPolicy.isAllowedForRequester(ticket.getStatus(), target)) {
            return;
        }
        throw new ForbiddenOperationException("Você não tem permissão para alterar o status deste chamado.");
    }

    private boolean isOwner(Ticket ticket, UserPrincipal principal) {
        return ticket.getRequester().getId().equals(principal.id());
    }
}
