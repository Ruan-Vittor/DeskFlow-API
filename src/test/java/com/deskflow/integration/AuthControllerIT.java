package com.deskflow.integration;

import com.deskflow.entity.Role;
import com.deskflow.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends IntegrationTestBase {

    @Test
    void deveRegistrarUsuarioPublicoComoRequesterERetornarToken() throws Exception {
        String body = """
                {
                  "name": "Maria Silva",
                  "email": "MARIA@DESKFLOW.TEST",
                  "password": "senhaSegura123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("maria@deskflow.test"))
                .andExpect(jsonPath("$.data.user.role").value("REQUESTER"));

        assertThat(userRepository.findByEmailIgnoreCase("maria@deskflow.test"))
                .isPresent()
                .get()
                .extracting(user -> user.getRole())
                .isEqualTo(Role.REQUESTER);
    }

    @Test
    void deveIgnorarTentativaDeEscolherPerfilNoCadastroPublico() throws Exception {
        String body = """
                {
                  "name": "Intruso",
                  "email": "intruso@deskflow.test",
                  "password": "senhaSegura123",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("REQUESTER"));

        assertThat(userRepository.findByEmailIgnoreCase("intruso@deskflow.test"))
                .get()
                .extracting(user -> user.getRole())
                .isEqualTo(Role.REQUESTER);
    }

    @Test
    void deveRetornarConflitoAoCadastrarEmailDuplicado() throws Exception {
        createRequester("duplicado@deskflow.test");

        String body = """
                {
                  "name": "Outro nome",
                  "email": "duplicado@deskflow.test",
                  "password": "senhaSegura123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.message").value("Já existe um usuário com este e-mail."));
    }

    @Test
    void deveAutenticarComCredenciaisValidas() throws Exception {
        createRequester("login@deskflow.test");

        String token = login("login@deskflow.test", "requester12345");

        assertThat(token).isNotBlank();
    }

    @Test
    void deveRejeitarSenhaIncorreta() throws Exception {
        createRequester("senha@deskflow.test");

        String body = """
                {"email":"senha@deskflow.test","password":"senhaErrada"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }

    @Test
    void deveRetornarBadRequestParaDadosInvalidos() throws Exception {
        String body = """
                {"name":"", "email":"email-invalido", "password":"123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    @Test
    void deveRetornarNaoAutorizadoSemToken() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarNaoAutorizadoComTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }
}
