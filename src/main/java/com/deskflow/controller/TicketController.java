package com.deskflow.controller;

import com.deskflow.dto.common.ApiResponse;
import com.deskflow.dto.common.PageResponse;
import com.deskflow.dto.ticket.AssignTicketRequest;
import com.deskflow.dto.ticket.CreateTicketRequest;
import com.deskflow.dto.ticket.TicketResponse;
import com.deskflow.dto.ticket.TicketSummaryResponse;
import com.deskflow.dto.ticket.UpdatePriorityRequest;
import com.deskflow.dto.ticket.UpdateTicketRequest;
import com.deskflow.dto.ticket.UpdateTicketStatusRequest;
import com.deskflow.entity.TicketCategory;
import com.deskflow.entity.TicketPriority;
import com.deskflow.entity.TicketStatus;
import com.deskflow.security.UserPrincipal;
import com.deskflow.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Chamados", description = "Abertura, consulta, atribuição e ciclo de vida dos chamados")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abrir um chamado")
    public ApiResponse<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Chamado aberto com sucesso.",
                ticketService.create(request, principal)
        );
    }

    @GetMapping
    @Operation(
            summary = "Listar chamados",
            description = "A listagem é filtrada automaticamente conforme o perfil do usuário autenticado."
    )
    public ApiResponse<PageResponse<TicketSummaryResponse>> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Chamados listados com sucesso.",
                ticketService.list(status, priority, category, pageable, principal)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar um chamado por ID")
    public ApiResponse<TicketResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Chamado consultado com sucesso.",
                ticketService.findById(id, principal)
        );
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar dados do chamado",
            description = "Permitido ao ADMIN e ao REQUESTER proprietário, enquanto o chamado não estiver encerrado."
    )
    public ApiResponse<TicketResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Chamado atualizado com sucesso.",
                ticketService.update(id, request, principal)
        );
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar o status do chamado")
    public ApiResponse<TicketResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Status do chamado atualizado com sucesso.",
                ticketService.updateStatus(id, request.status(), principal)
        );
    }

    @PatchMapping("/{id}/assign")
    @Operation(
            summary = "Atribuir o chamado a um técnico",
            description = "ADMIN pode atribuir a qualquer técnico; TECHNICIAN pode atribuir apenas a si mesmo."
    )
    public ApiResponse<TicketResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Chamado atribuído com sucesso.",
                ticketService.assign(id, request.technicianId(), principal)
        );
    }

    @PatchMapping("/{id}/priority")
    @Operation(
            summary = "Alterar a prioridade do chamado",
            description = "Permitido ao ADMIN e ao técnico responsável pelo chamado."
    )
    public ApiResponse<TicketResponse> updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriorityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Prioridade do chamado atualizada com sucesso.",
                ticketService.updatePriority(id, request.priority(), principal)
        );
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Encerrar um chamado")
    public ApiResponse<TicketResponse> close(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Chamado encerrado com sucesso.",
                ticketService.close(id, principal)
        );
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reabrir um chamado encerrado ou resolvido")
    public ApiResponse<TicketResponse> reopen(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Chamado reaberto com sucesso.",
                ticketService.reopen(id, principal)
        );
    }
}
