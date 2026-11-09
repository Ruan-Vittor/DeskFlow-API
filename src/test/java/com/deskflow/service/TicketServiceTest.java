package com.deskflow.service;

import com.deskflow.dto.ticket.CreateTicketRequest;
import com.deskflow.dto.ticket.TicketResponse;
import com.deskflow.dto.ticket.UpdateTicketRequest;
import com.deskflow.entity.Ticket;
import com.deskflow.entity.TicketCategory;
import com.deskflow.entity.TicketPriority;
import com.deskflow.entity.TicketStatus;
import com.deskflow.entity.User;
import com.deskflow.exception.BusinessRuleException;
import com.deskflow.exception.ForbiddenOperationException;
import com.deskflow.exception.InvalidStatusTransitionException;
import com.deskflow.exception.ResourceNotFoundException;
import com.deskflow.mapper.TicketMapper;
import com.deskflow.mapper.UserMapper;
import com.deskflow.repository.TicketRepository;
import com.deskflow.service.support.PageableSanitizer;
import com.deskflow.service.support.TicketAccessPolicy;
import com.deskflow.service.support.TicketStatusTransitionPolicy;
import com.deskflow.support.TestFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserService userService;

    private TicketService ticketService;

    private final User admin = TestFixtures.admin(1L);
    private final User technician = TestFixtures.technician(2L);
    private final User requester = TestFixtures.requester(4L);

    @BeforeEach
    void setUp() {
        TicketAccessPolicy accessPolicy =
                new TicketAccessPolicy(new TicketStatusTransitionPolicy());
        ticketService = new TicketService(
                ticketRepository,
                userService,
                accessPolicy,
                new TicketStatusTransitionPolicy(),
                new TicketMapper(new UserMapper()),
                new PageableSanitizer()
        );
    }

    @Test
    void deveCriarChamadoAbertoParaUsuarioAutenticado() {
        when(userService.getActiveUser(4L)).thenReturn(requester);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ReflectionTestUtils.setField(ticket, "id", 100L);
            return ticket;
        });

        TicketResponse response = ticketService.create(
                new CreateTicketRequest(
                        " Notebook não liga ",
                        " Descrição detalhada do problema. ",
                        TicketCategory.HARDWARE,
                        TicketPriority.HIGH
                ),
                TestFixtures.principal(requester)
        );

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.requester().id()).isEqualTo(4L);
        assertThat(response.technician()).isNull();
        assertThat(response.title()).isEqualTo("Notebook não liga");
    }

    @Test
    void deveImpedirTransicaoDeStatusInvalida() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateStatus(
                100L, TicketStatus.RESOLVED, TestFixtures.principal(admin)
        )).isInstanceOf(InvalidStatusTransitionException.class);

        verify(ticketRepository, never()).saveAndFlush(any(Ticket.class));
    }

    @Test
    void requesterNaoDeveAlterarStatusForaDasTransicoesPermitidas() {
        Ticket ticket = TestFixtures.ticketWithStatus(
                100L, requester, TicketStatus.IN_PROGRESS
        );
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateStatus(
                100L, TicketStatus.RESOLVED, TestFixtures.principal(requester)
        )).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminDeveAtribuirChamadoATecnico() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));
        when(userService.getActiveTechnician(2L)).thenReturn(technician);
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.assign(
                100L, 2L, TestFixtures.principal(admin)
        );

        assertThat(response.technician().id()).isEqualTo(2L);
        assertThat(ticket.getTechnician()).isEqualTo(technician);
    }

    @Test
    void tecnicoNaoDeveAtribuirChamadoAOutroTecnico() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.assign(
                100L, 3L, TestFixtures.principal(technician)
        )).isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("si mesmos");

        verify(userService, never()).getActiveTechnician(any());
    }

    @Test
    void deveRejeitarAtribuicaoAUsuarioSemPerfilTecnico() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));
        when(userService.getActiveTechnician(5L))
                .thenThrow(new BusinessRuleException("Perfil inválido."));

        assertThatThrownBy(() -> ticketService.assign(
                100L, 5L, TestFixtures.principal(admin)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void naoDeveEditarChamadoEncerrado() {
        Ticket ticket = TestFixtures.ticketWithStatus(100L, requester, TicketStatus.CLOSED);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));

        UpdateTicketRequest request = new UpdateTicketRequest(
                "Novo título",
                "Nova descrição do chamado encerrado.",
                TicketCategory.SOFTWARE,
                TicketPriority.LOW
        );

        assertThatThrownBy(() -> ticketService.update(
                100L, request, TestFixtures.principal(requester)
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("encerrado");
    }

    @Test
    void requesterDeveAtualizarProprioChamadoAberto() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.update(
                100L,
                new UpdateTicketRequest(
                        " Título atualizado ",
                        " Descrição atualizada do chamado. ",
                        TicketCategory.SOFTWARE,
                        TicketPriority.CRITICAL
                ),
                TestFixtures.principal(requester)
        );

        assertThat(response.title()).isEqualTo("Título atualizado");
        assertThat(response.category()).isEqualTo(TicketCategory.SOFTWARE);
        assertThat(response.priority()).isEqualTo(TicketPriority.CRITICAL);
    }

    @Test
    void adminDeveAlterarPrioridade() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.updatePriority(
                100L, TicketPriority.CRITICAL, TestFixtures.principal(admin)
        );

        assertThat(response.priority()).isEqualTo(TicketPriority.CRITICAL);
    }

    @Test
    void requesterNaoDeveAlterarPrioridadePeloEndpointEspecifico() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updatePriority(
                100L, TicketPriority.HIGH, TestFixtures.principal(requester)
        )).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void requesterDeveListarApenasPropriosChamados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(TestFixtures.ticket(100L, requester)));
        when(ticketRepository.searchByRequester(
                eq(4L), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(page);

        var response = ticketService.list(
                null, null, null, pageable, TestFixtures.principal(requester)
        );

        assertThat(response.totalElements()).isEqualTo(1);
        verify(ticketRepository).searchByRequester(
                eq(4L), isNull(), isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    void deveRetornarNotFoundParaChamadoInexistente() {
        when(ticketRepository.findDetailedById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.findById(
                999L, TestFixtures.principal(requester)
        )).isInstanceOf(ResourceNotFoundException.class);
    }
}
