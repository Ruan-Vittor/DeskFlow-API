package com.deskflow.support;

import com.deskflow.entity.Role;
import com.deskflow.entity.Ticket;
import com.deskflow.entity.TicketCategory;
import com.deskflow.entity.TicketPriority;
import com.deskflow.entity.User;
import com.deskflow.security.UserPrincipal;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user(Long id, String name, String email, Role role) {
        User user = new User(name, email, "encoded-password", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static User admin(Long id) {
        return user(id, "Admin", "admin" + id + "@deskflow.test", Role.ADMIN);
    }

    public static User technician(Long id) {
        return user(id, "Técnico", "tech" + id + "@deskflow.test", Role.TECHNICIAN);
    }

    public static User requester(Long id) {
        return user(id, "Solicitante", "requester" + id + "@deskflow.test", Role.REQUESTER);
    }

    public static UserPrincipal principal(User user) {
        return UserPrincipal.from(user);
    }

    public static Ticket ticket(Long id, User requester) {
        Ticket ticket = new Ticket(
                "Título do chamado",
                "Descrição detalhada do chamado de teste.",
                TicketCategory.HARDWARE,
                TicketPriority.MEDIUM,
                requester
        );
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }

    public static Ticket ticketWithStatus(Long id, User requester, com.deskflow.entity.TicketStatus status) {
        Ticket ticket = ticket(id, requester);
        ticket.changeStatus(status);
        return ticket;
    }
}
