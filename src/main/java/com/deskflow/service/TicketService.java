package com.deskflow.service;

import com.deskflow.dto.common.PageResponse;
import com.deskflow.dto.ticket.CreateTicketRequest;
import com.deskflow.dto.ticket.TicketResponse;
import com.deskflow.dto.ticket.TicketSummaryResponse;
import com.deskflow.dto.ticket.UpdateTicketRequest;
import com.deskflow.entity.Ticket;
import com.deskflow.entity.TicketCategory;
import com.deskflow.entity.TicketPriority;
import com.deskflow.entity.TicketStatus;
import com.deskflow.entity.Role;
import com.deskflow.entity.User;
import com.deskflow.exception.ForbiddenOperationException;
import com.deskflow.exception.ResourceNotFoundException;
import com.deskflow.mapper.TicketMapper;
import com.deskflow.repository.TicketRepository;
import com.deskflow.security.UserPrincipal;
import com.deskflow.service.support.PageableSanitizer;
import com.deskflow.service.support.TicketAccessPolicy;
import com.deskflow.service.support.TicketStatusTransitionPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final TicketAccessPolicy accessPolicy;
    private final TicketStatusTransitionPolicy transitionPolicy;
    private final TicketMapper ticketMapper;
    private final PageableSanitizer pageableSanitizer;

    public TicketService(TicketRepository ticketRepository, UserService userService,
                         TicketAccessPolicy accessPolicy, TicketStatusTransitionPolicy transitionPolicy,
                         TicketMapper ticketMapper, PageableSanitizer pageableSanitizer) {
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
        this.transitionPolicy = transitionPolicy;
        this.ticketMapper = ticketMapper;
        this.pageableSanitizer = pageableSanitizer;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, UserPrincipal principal) {
        User requester = userService.getActiveUser(principal.id());
        Ticket ticket = new Ticket(
                request.title().trim(),
                request.description().trim(),
                request.category(),
                request.priority(),
                requester
        );
        ticketRepository.save(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id, UserPrincipal principal) {
        Ticket ticket = getDetailedTicket(id);
        accessPolicy.assertCanView(ticket, principal);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> list(TicketStatus status, TicketPriority priority,
                                                    TicketCategory category, Pageable pageable,
                                                    UserPrincipal principal) {
        Pageable safePageable = pageableSanitizer.forTickets(pageable);

        Page<Ticket> tickets = switch (principal.role()) {
            case ADMIN -> ticketRepository.searchAll(
                    status, priority, category, safePageable
            );
            case REQUESTER -> ticketRepository.searchByRequester(
                    principal.id(), status, priority, category, safePageable
            );
            case TECHNICIAN -> ticketRepository.searchForTechnician(
                    principal.id(), status, priority, category, safePageable
            );
        };

        return PageResponse.from(tickets.map(ticketMapper::toSummary));
    }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest request, UserPrincipal principal) {
        Ticket ticket = getDetailedTicket(id);
        accessPolicy.assertCanEdit(ticket, principal);

        ticket.updateDetails(
                request.title().trim(),
                request.description().trim(),
                request.category(),
                request.priority()
        );
        ticketRepository.saveAndFlush(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateStatus(Long id, TicketStatus targetStatus, UserPrincipal principal) {
        Ticket ticket = getDetailedTicket(id);
        transitionPolicy.assertValidTransition(ticket.getStatus(), targetStatus);
        accessPolicy.assertCanChangeStatus(ticket, targetStatus, principal);

        ticket.changeStatus(targetStatus);
        ticketRepository.saveAndFlush(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse close(Long id, UserPrincipal principal) {
        return updateStatus(id, TicketStatus.CLOSED, principal);
    }

    @Transactional
    public TicketResponse reopen(Long id, UserPrincipal principal) {
        return updateStatus(id, TicketStatus.OPEN, principal);
    }

    @Transactional
    public TicketResponse assign(Long id, Long technicianId, UserPrincipal principal) {
        Ticket ticket = getDetailedTicket(id);
        accessPolicy.assertCanAssign(ticket, principal);

        if (principal.role() == Role.TECHNICIAN
                && !principal.id().equals(technicianId)) {
            throw new ForbiddenOperationException(
                    "Técnicos só podem atribuir chamados a si mesmos."
            );
        }

        User technician = userService.getActiveTechnician(technicianId);
        ticket.assignTo(technician);
        ticketRepository.saveAndFlush(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse updatePriority(Long id, TicketPriority priority, UserPrincipal principal) {
        Ticket ticket = getDetailedTicket(id);
        accessPolicy.assertCanChangePriority(ticket, principal);

        ticket.changePriority(priority);
        ticketRepository.saveAndFlush(ticket);
        return ticketMapper.toResponse(ticket);
    }

    private Ticket getDetailedTicket(Long id) {
        return ticketRepository.findDetailedById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Chamado", id));
    }
}
