package com.deskflow.integration;

import com.deskflow.entity.Ticket;
import com.deskflow.entity.TicketPriority;
import com.deskflow.entity.TicketStatus;
import com.deskflow.entity.User;
import com.deskflow.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketControllerIT extends IntegrationTestBase {

    private User admin;
    private User technician;
    private User otherTechnician;
    private User requester;
    private User otherRequester;

    private String adminToken;
    private String technicianToken;
    private String otherTechnicianToken;
    private String requesterToken;
    private String otherRequesterToken;

    @BeforeEach
    void setUpUsers() throws Exception {
        admin = createAdmin();
        technician = createTechnician("technician@deskflow.test");
        otherTechnician = createTechnician("other.technician@deskflow.test");
        requester = createRequester("requester@deskflow.test");
        otherRequester = createRequester("other.requester@deskflow.test");

        adminToken = login("admin@deskflow.test", "admin12345");
        technicianToken = login("technician@deskflow.test", "tecnico12345");
        otherTechnicianToken = login("other.technician@deskflow.test", "tecnico12345");
        requesterToken = login("requester@deskflow.test", "requester12345");
        otherRequesterToken = login("other.requester@deskflow.test", "requester12345");
    }

    @Test
    void deveAbrirChamadoComStatusOpenERequesterAtual() throws Exception {
        String body = """
                {
                  "title":"Notebook não liga",
                  "description":"O notebook não liga desde a última atualização.",
                  "category":"HARDWARE",
                  "priority":"HIGH"
                }
                """;

        MvcResult result = mockMvc.perform(authorized(post("/api/tickets"), requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.requester.email").value("requester@deskflow.test"))
                .andExpect(jsonPath("$.data.technician").doesNotExist())
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(authorized(get("/api/tickets/" + id.longValue()), requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Notebook não liga"));
    }

    @Test
    void requesterNaoDeveVisualizarChamadoDeOutroUsuario() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        mockMvc.perform(authorized(get("/api/tickets/" + ticketId), otherRequesterToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Você não tem permissão para acessar este chamado."));
    }

    @Test
    void adminDeveVisualizarQualquerChamado() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        mockMvc.perform(authorized(get("/api/tickets/" + ticketId), adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void tecnicoDeveVisualizarChamadoNaoAtribuido() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        mockMvc.perform(authorized(get("/api/tickets/" + ticketId), technicianToken))
                .andExpect(status().isOk());
    }

    @Test
    void tecnicoNaoDeveVisualizarChamadoAtribuidoAOutroTecnico() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);
        assign(ticketId, technician.getId(), adminToken)
                .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/tickets/" + ticketId), otherTechnicianToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void requesterDeveListarApenasSeusChamados() throws Exception {
        createOpenTicket(requesterToken);
        createOpenTicket(requesterToken);
        createOpenTicket(otherRequesterToken);

        mockMvc.perform(authorized(get("/api/tickets?size=100"), requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].requester.email")
                        .value("requester@deskflow.test"));

        mockMvc.perform(authorized(get("/api/tickets?size=100"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    void tecnicoDeveListarDisponiveisEAtribuidosASi() throws Exception {
        Long unassigned = createOpenTicket(requesterToken);
        Long assignedToTechnician = createOpenTicket(requesterToken);
        Long assignedToOther = createOpenTicket(requesterToken);

        assign(assignedToTechnician, technician.getId(), adminToken)
                .andExpect(status().isOk());
        assign(assignedToOther, otherTechnician.getId(), adminToken)
                .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/tickets?size=100"), technicianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        assertThat(unassigned).isNotNull();
    }

    @Test
    void adminDeveAtribuirChamadoATecnico() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        assign(ticketId, technician.getId(), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.technician.email")
                        .value("technician@deskflow.test"));
    }

    @Test
    void tecnicoDevePoderSeAutoatribuir() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        assign(ticketId, technician.getId(), technicianToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.technician.id").value(technician.getId().intValue()));
    }

    @Test
    void tecnicoNaoDeveAtribuirChamadoAOutroTecnico() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        assign(ticketId, otherTechnician.getId(), technicianToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Técnicos só podem atribuir chamados a si mesmos."));
    }

    @Test
    void requesterNaoDeveAtribuirChamado() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        assign(ticketId, technician.getId(), requesterToken)
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoDeveAtribuirChamadoAUsuarioQueNaoSejaTecnico() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        assign(ticketId, requester.getId(), adminToken)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("O usuário informado não possui o perfil TECHNICIAN."));
    }

    @Test
    void deveImpedirTransicaoDeStatusInvalida() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        updateStatus(ticketId, TicketStatus.RESOLVED, adminToken)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("Transição de status inválida: OPEN -> RESOLVED"));
    }

    @Test
    void tecnicoResponsavelDeveConduzirCicloCompletoDoChamado() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);
        assign(ticketId, technician.getId(), adminToken).andExpect(status().isOk());

        updateStatus(ticketId, TicketStatus.IN_PROGRESS, technicianToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        updateStatus(ticketId, TicketStatus.WAITING_FOR_USER, technicianToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_FOR_USER"));

        updateStatus(ticketId, TicketStatus.IN_PROGRESS, technicianToken)
                .andExpect(status().isOk());

        updateStatus(ticketId, TicketStatus.RESOLVED, technicianToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(authorized(post("/api/tickets/" + ticketId + "/close"), technicianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty());
    }

    @Test
    void requesterDeveEncerrarProprioChamadoAberto() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        mockMvc.perform(authorized(post("/api/tickets/" + ticketId + "/close"), requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void requesterDeveReabrirProprioChamadoEncerrado() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        mockMvc.perform(authorized(post("/api/tickets/" + ticketId + "/close"), requesterToken))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/tickets/" + ticketId + "/reopen"), requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.closedAt").doesNotExist());
    }

    @Test
    void requesterNaoDeveResolverProprioChamado() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);
        assign(ticketId, technician.getId(), adminToken).andExpect(status().isOk());
        updateStatus(ticketId, TicketStatus.IN_PROGRESS, technicianToken)
                .andExpect(status().isOk());

        updateStatus(ticketId, TicketStatus.RESOLVED, requesterToken)
                .andExpect(status().isForbidden());
    }

    @Test
    void chamadoEncerradoNaoDeveSerEditado() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);
        updateStatus(ticketId, TicketStatus.CLOSED, adminToken).andExpect(status().isOk());

        String body = """
                {
                  "title":"Título alterado",
                  "description":"Descrição alterada do chamado encerrado.",
                  "category":"SOFTWARE",
                  "priority":"LOW"
                }
                """;

        mockMvc.perform(authorized(put("/api/tickets/" + ticketId), requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("Um chamado encerrado não pode ser editado."));
    }

    @Test
    void adminDeveAlterarPrioridade() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        updatePriority(ticketId, TicketPriority.CRITICAL, adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));
    }

    @Test
    void tecnicoResponsavelDeveAlterarPrioridade() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);
        assign(ticketId, technician.getId(), adminToken).andExpect(status().isOk());

        updatePriority(ticketId, TicketPriority.HIGH, technicianToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    void requesterNaoDeveAlterarPrioridadePeloEndpointEspecifico() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        updatePriority(ticketId, TicketPriority.HIGH, requesterToken)
                .andExpect(status().isForbidden());
    }

    @Test
    void deveAplicarFiltrosEPaginacao() throws Exception {
        createTicket(requesterToken, "Chamado A", TicketPriority.HIGH,
                com.deskflow.entity.TicketCategory.HARDWARE);
        createTicket(requesterToken, "Chamado B", TicketPriority.LOW,
                com.deskflow.entity.TicketCategory.SOFTWARE);
        createTicket(requesterToken, "Chamado C", TicketPriority.HIGH,
                com.deskflow.entity.TicketCategory.NETWORK);

        mockMvc.perform(authorized(
                        get("/api/tickets?priority=HIGH&page=0&size=1&sort=title,asc"),
                        requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Chamado A"))
                .andExpect(jsonPath("$.data.content[0].priority").value("HIGH"));
    }

    @Test
    void deveFiltrarPorStatus() throws Exception {
        Long closedTicket = createOpenTicket(requesterToken);
        createOpenTicket(requesterToken);
        updateStatus(closedTicket, TicketStatus.CLOSED, adminToken).andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/tickets?status=CLOSED"), requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("CLOSED"));
    }

    @Test
    void deveRejeitarCampoDeOrdenacaoInvalido() throws Exception {
        mockMvc.perform(authorized(get("/api/tickets?sort=campoInexistente,asc"), requesterToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("Campo de ordenação inválido: campoInexistente"));
    }

    @Test
    void deveRetornarNotFoundParaChamadoInexistente() throws Exception {
        mockMvc.perform(authorized(get("/api/tickets/999999"), requesterToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Chamado não encontrado(a) com id 999999"));
    }

    @Test
    void deveRejeitarDadosInvalidosNaCriacao() throws Exception {
        String body = """
                {"title":"", "description":"curta", "category":null, "priority":null}
                """;

        mockMvc.perform(authorized(post("/api/tickets"), requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.description").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.category").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.priority").isNotEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions assign(Long ticketId, Long technicianId,
                                                                       String token) throws Exception {
        String body = """
                {"technicianId": %d}
                """.formatted(technicianId);

        return mockMvc.perform(authorized(patch("/api/tickets/" + ticketId + "/assign"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions updateStatus(Long ticketId,
                                                                           TicketStatus status,
                                                                           String token) throws Exception {
        String body = """
                {"status":"%s"}
                """.formatted(status.name());

        return mockMvc.perform(authorized(patch("/api/tickets/" + ticketId + "/status"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions updatePriority(Long ticketId,
                                                                             TicketPriority priority,
                                                                             String token) throws Exception {
        String body = """
                {"priority":"%s"}
                """.formatted(priority.name());

        return mockMvc.perform(authorized(patch("/api/tickets/" + ticketId + "/priority"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @SuppressWarnings("unused")
    private Ticket ticket(Long id) {
        return ticketEntity(id);
    }
}
