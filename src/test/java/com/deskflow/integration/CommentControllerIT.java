package com.deskflow.integration;

import com.deskflow.entity.TicketStatus;
import com.deskflow.entity.User;
import com.deskflow.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentControllerIT extends IntegrationTestBase {

    private User admin;
    private User technician;
    private User requester;
    private User otherRequester;

    private String adminToken;
    private String technicianToken;
    private String requesterToken;
    private String otherRequesterToken;

    @BeforeEach
    void setUpUsers() throws Exception {
        admin = createAdmin();
        technician = createTechnician("technician@deskflow.test");
        requester = createRequester("requester@deskflow.test");
        otherRequester = createRequester("other.requester@deskflow.test");

        adminToken = login("admin@deskflow.test", "admin12345");
        technicianToken = login("technician@deskflow.test", "tecnico12345");
        requesterToken = login("requester@deskflow.test", "requester12345");
        otherRequesterToken = login("other.requester@deskflow.test", "requester12345");
    }

    @Test
    void requesterDeveComentarEListarComentariosDoProprioChamado() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        addComment(ticketId, "Já reiniciei o equipamento, mas o problema continua.", requesterToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ticketId").value(ticketId.intValue()))
                .andExpect(jsonPath("$.data.author.email").value("requester@deskflow.test"))
                .andExpect(jsonPath("$.data.content")
                        .value("Já reiniciei o equipamento, mas o problema continua."));

        mockMvc.perform(authorized(get("/api/tickets/" + ticketId + "/comments"), requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].author.email")
                        .value("requester@deskflow.test"));
    }

    @Test
    void outroRequesterNaoDeveComentarNemListarComentarios() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        addComment(ticketId, "Tentativa de acesso indevido.", otherRequesterToken)
                .andExpect(status().isForbidden());

        mockMvc.perform(authorized(
                        get("/api/tickets/" + ticketId + "/comments"), otherRequesterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnicoDeveComentarEmChamadoNaoAtribuido() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        addComment(ticketId, "Vou assumir este atendimento.", technicianToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.author.role").value("TECHNICIAN"));
    }

    @Test
    void chamadoEncerradoNaoDeveAceitarComentarios() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);
        closeTicket(ticketId, adminToken);

        addComment(ticketId, "Comentário após encerramento.", requesterToken)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("Um chamado encerrado não pode receber novos comentários."));

        mockMvc.perform(authorized(get("/api/tickets/" + ticketId + "/comments"), requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void deveRejeitarComentarioVazio() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        addComment(ticketId, "   ", requesterToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").isNotEmpty());
    }

    @Test
    void deveListarComentariosEmOrdemCronologica() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        addComment(ticketId, "Primeiro comentário.", requesterToken)
                .andExpect(status().isCreated());
        addComment(ticketId, "Segundo comentário.", technicianToken)
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/tickets/" + ticketId + "/comments"), requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("Primeiro comentário."))
                .andExpect(jsonPath("$.data.content[1].content").value("Segundo comentário."));
    }

    @Test
    void deveRetornarNotFoundAoComentarEmChamadoInexistente() throws Exception {
        addComment(999999L, "Comentário em chamado inexistente.", requesterToken)
                .andExpect(status().isNotFound());
    }

    @Test
    void adminDeveComentarEmQualquerChamado() throws Exception {
        Long ticketId = createOpenTicket(requesterToken);

        addComment(ticketId, "Acompanhamento administrativo.", adminToken)
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions addComment(Long ticketId, String content,
                                                                          String token) throws Exception {
        String body = """
                {"content":"%s"}
                """.formatted(content);

        return mockMvc.perform(authorized(
                post("/api/tickets/" + ticketId + "/comments"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void closeTicket(Long ticketId, String token) throws Exception {
        mockMvc.perform(authorized(post("/api/tickets/" + ticketId + "/close"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(TicketStatus.CLOSED.name()));
    }
}
