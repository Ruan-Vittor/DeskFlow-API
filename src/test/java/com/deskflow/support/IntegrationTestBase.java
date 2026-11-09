package com.deskflow.support;

import com.deskflow.entity.Role;
import com.deskflow.entity.Ticket;
import com.deskflow.entity.TicketCategory;
import com.deskflow.entity.TicketPriority;
import com.deskflow.entity.User;
import com.deskflow.repository.CommentRepository;
import com.deskflow.repository.TicketRepository;
import com.deskflow.repository.UserRepository;
import com.deskflow.security.UserPrincipal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TicketRepository ticketRepository;

    @Autowired
    protected CommentRepository commentRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        commentRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    protected User createUser(String name, String email, String rawPassword, Role role) {
        return userRepository.saveAndFlush(
                new User(name, email.toLowerCase(), passwordEncoder.encode(rawPassword), role)
        );
    }

    protected User createAdmin() {
        return createUser("Administrador", "admin@deskflow.test", "admin12345", Role.ADMIN);
    }

    protected User createTechnician(String email) {
        return createUser("Técnico", email, "tecnico12345", Role.TECHNICIAN);
    }

    protected User createRequester(String email) {
        return createUser("Solicitante", email, "requester12345", Role.REQUESTER);
    }

    protected String login(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    protected MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder,
                                                       String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    protected Long createTicket(String token, String title, TicketPriority priority,
                                TicketCategory category) throws Exception {
        String body = """
                {
                  "title": "%s",
                  "description": "Descrição detalhada do problema informado.",
                  "category": "%s",
                  "priority": "%s"
                }
                """.formatted(title, category.name(), priority.name());

        MvcResult result = mockMvc.perform(authorized(post("/api/tickets"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    protected Long createOpenTicket(String token) throws Exception {
        return createTicket(token, "Problema no computador", TicketPriority.MEDIUM,
                TicketCategory.HARDWARE);
    }

    protected Ticket ticketEntity(Long id) {
        return ticketRepository.findById(id).orElseThrow();
    }

    protected UserPrincipal principalOf(User user) {
        return UserPrincipal.from(user);
    }
}
