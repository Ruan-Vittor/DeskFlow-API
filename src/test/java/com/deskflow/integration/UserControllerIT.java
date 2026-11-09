package com.deskflow.integration;

import com.deskflow.entity.Role;
import com.deskflow.entity.User;
import com.deskflow.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIT extends IntegrationTestBase {

    private User admin;
    private User requester;
    private String adminToken;
    private String requesterToken;

    @BeforeEach
    void setUpUsers() throws Exception {
        admin = createAdmin();
        requester = createRequester("requester@deskflow.test");
        createTechnician("technician@deskflow.test");

        adminToken = login("admin@deskflow.test", "admin12345");
        requesterToken = login("requester@deskflow.test", "requester12345");
    }

    @Test
    void deveConsultarPerfilDoUsuarioAutenticado() throws Exception {
        mockMvc.perform(authorized(get("/api/users/me"), requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("requester@deskflow.test"))
                .andExpect(jsonPath("$.data.role").value("REQUESTER"));
    }

    @Test
    void deveAtualizarNomeDoPerfil() throws Exception {
        String body = """
                {"name":"Solicitante Atualizado"}
                """;

        mockMvc.perform(authorized(put("/api/users/me"), requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Solicitante Atualizado"));

        assertThat(userRepository.findById(requester.getId()).orElseThrow().getName())
                .isEqualTo("Solicitante Atualizado");
    }

    @Test
    void deveExigirSenhaAtualParaAlterarSenha() throws Exception {
        String body = """
                {"name":"Solicitante", "newPassword":"novaSenha123"}
                """;

        mockMvc.perform(authorized(put("/api/users/me"), requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("A senha atual é obrigatória para alterar a senha."));
    }

    @Test
    void deveRejeitarSenhaAtualIncorreta() throws Exception {
        String body = """
                {
                  "name":"Solicitante",
                  "currentPassword":"senhaErrada",
                  "newPassword":"novaSenha123"
                }
                """;

        mockMvc.perform(authorized(put("/api/users/me"), requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("A senha atual informada está incorreta."));
    }

    @Test
    void deveAlterarSenhaComSenhaAtualCorreta() throws Exception {
        String body = """
                {
                  "name":"Solicitante",
                  "currentPassword":"requester12345",
                  "newPassword":"novaSenha123"
                }
                """;

        mockMvc.perform(authorized(put("/api/users/me"), requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        String newToken = login("requester@deskflow.test", "novaSenha123");
        assertThat(newToken).isNotBlank();
    }

    @Test
    void adminDeveListarUsuariosComPaginacao() throws Exception {
        mockMvc.perform(authorized(get("/api/users?page=0&size=2&sort=name,asc"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    void requesterNaoDeveListarUsuarios() throws Exception {
        mockMvc.perform(authorized(get("/api/users"), requesterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDeveCriarTecnico() throws Exception {
        String body = """
                {
                  "name":"Novo Técnico",
                  "email":"novo.tech@deskflow.test",
                  "password":"tecnico12345",
                  "role":"TECHNICIAN"
                }
                """;

        mockMvc.perform(authorized(post("/api/users"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.data.email").value("novo.tech@deskflow.test"));

        assertThat(userRepository.findByEmailIgnoreCase("novo.tech@deskflow.test"))
                .get()
                .extracting(User::getRole)
                .isEqualTo(Role.TECHNICIAN);
    }

    @Test
    void requesterNaoDeveCriarUsuarios() throws Exception {
        String body = """
                {
                  "name":"Outro Técnico",
                  "email":"outro.tech@deskflow.test",
                  "password":"tecnico12345",
                  "role":"TECHNICIAN"
                }
                """;

        mockMvc.perform(authorized(post("/api/users"), requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoDeveCriarUsuarioComEmailDuplicado() throws Exception {
        String body = """
                {
                  "name":"Duplicado",
                  "email":"requester@deskflow.test",
                  "password":"tecnico12345",
                  "role":"TECHNICIAN"
                }
                """;

        mockMvc.perform(authorized(post("/api/users"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}
