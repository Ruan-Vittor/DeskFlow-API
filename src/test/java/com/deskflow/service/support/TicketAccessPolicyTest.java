package com.deskflow.service.support;

import com.deskflow.entity.Ticket;
import com.deskflow.entity.TicketStatus;
import com.deskflow.entity.User;
import com.deskflow.exception.BusinessRuleException;
import com.deskflow.exception.ForbiddenOperationException;
import com.deskflow.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketAccessPolicyTest {

    private final TicketAccessPolicy policy =
            new TicketAccessPolicy(new TicketStatusTransitionPolicy());

    private final User admin = TestFixtures.admin(1L);
    private final User technician = TestFixtures.technician(2L);
    private final User otherTechnician = TestFixtures.technician(3L);
    private final User requester = TestFixtures.requester(4L);
    private final User otherRequester = TestFixtures.requester(5L);

    @Test
    void devePermitirVisualizacaoConformePerfilEPropriedade() {
        Ticket ticket = TestFixtures.ticket(100L, requester);

        assertThat(policy.canView(ticket, TestFixtures.principal(admin))).isTrue();
        assertThat(policy.canView(ticket, TestFixtures.principal(requester))).isTrue();
        assertThat(policy.canView(ticket, TestFixtures.principal(otherRequester))).isFalse();
        assertThat(policy.canView(ticket, TestFixtures.principal(technician))).isTrue();
    }

    @Test
    void tecnicoNaoDeveVisualizarChamadoAtribuidoAOutro() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        ticket.assignTo(otherTechnician);

        assertThat(policy.canView(ticket, TestFixtures.principal(technician))).isFalse();
        assertThat(policy.canView(ticket, TestFixtures.principal(otherTechnician))).isTrue();
    }

    @Test
    void devePermitirEdicaoApenasAoAdminEOwner() {
        Ticket ticket = TestFixtures.ticket(100L, requester);

        assertThatCode(() -> policy.assertCanEdit(ticket, TestFixtures.principal(admin)))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.assertCanEdit(ticket, TestFixtures.principal(requester)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.assertCanEdit(ticket, TestFixtures.principal(otherRequester)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> policy.assertCanEdit(ticket, TestFixtures.principal(technician)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void naoDeveEditarChamadoEncerrado() {
        Ticket ticket = TestFixtures.ticketWithStatus(100L, requester, TicketStatus.CLOSED);

        assertThatThrownBy(() -> policy.assertCanEdit(ticket, TestFixtures.principal(admin)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("encerrado");
    }

    @Test
    void naoDeveComentarEmChamadoEncerrado() {
        Ticket ticket = TestFixtures.ticketWithStatus(100L, requester, TicketStatus.CLOSED);

        assertThatThrownBy(() -> policy.assertCanComment(ticket, TestFixtures.principal(requester)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("comentários");
    }

    @Test
    void naoDeveComentarSemPermissaoDeVisualizacao() {
        Ticket ticket = TestFixtures.ticket(100L, requester);

        assertThatThrownBy(() -> policy.assertCanComment(
                ticket, TestFixtures.principal(otherRequester)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void devePermitirAtribuicaoApenasParaTecnicoEAdmin() {
        Ticket ticket = TestFixtures.ticket(100L, requester);

        assertThatCode(() -> policy.assertCanAssign(ticket, TestFixtures.principal(admin)))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.assertCanAssign(ticket, TestFixtures.principal(technician)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.assertCanAssign(ticket, TestFixtures.principal(requester)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void tecnicoNaoDeveAlterarStatusDeChamadoNaoAtribuido() {
        Ticket ticket = TestFixtures.ticket(100L, requester);

        assertThatThrownBy(() -> policy.assertCanChangeStatus(
                ticket, TicketStatus.IN_PROGRESS, TestFixtures.principal(technician)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void tecnicoResponsavelDeveAlterarStatus() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        ticket.assignTo(technician);

        assertThatCode(() -> policy.assertCanChangeStatus(
                ticket, TicketStatus.IN_PROGRESS, TestFixtures.principal(technician)))
                .doesNotThrowAnyException();
    }

    @Test
    void requesterDeveAlterarStatusSomenteNasTransicoesPermitidas() {
        Ticket open = TestFixtures.ticket(100L, requester);
        Ticket inProgress = TestFixtures.ticketWithStatus(101L, requester, TicketStatus.IN_PROGRESS);
        Ticket waiting = TestFixtures.ticketWithStatus(102L, requester, TicketStatus.WAITING_FOR_USER);

        assertThatCode(() -> policy.assertCanChangeStatus(
                open, TicketStatus.CLOSED, TestFixtures.principal(requester)))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.assertCanChangeStatus(
                waiting, TicketStatus.IN_PROGRESS, TestFixtures.principal(requester)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.assertCanChangeStatus(
                open, TicketStatus.IN_PROGRESS, TestFixtures.principal(requester)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> policy.assertCanChangeStatus(
                inProgress, TicketStatus.RESOLVED, TestFixtures.principal(requester)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void prioridadeDeveSerAlteradaPorAdminOuTecnicoResponsavel() {
        Ticket unassigned = TestFixtures.ticket(100L, requester);
        Ticket assigned = TestFixtures.ticket(101L, requester);
        assigned.assignTo(technician);

        assertThatCode(() -> policy.assertCanChangePriority(
                unassigned, TestFixtures.principal(admin)))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.assertCanChangePriority(
                assigned, TestFixtures.principal(technician)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.assertCanChangePriority(
                unassigned, TestFixtures.principal(technician)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> policy.assertCanChangePriority(
                unassigned, TestFixtures.principal(requester)))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
