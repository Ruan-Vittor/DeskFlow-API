package com.deskflow.service;

import com.deskflow.dto.comment.CommentResponse;
import com.deskflow.dto.comment.CreateCommentRequest;
import com.deskflow.dto.common.PageResponse;
import com.deskflow.entity.Comment;
import com.deskflow.entity.Ticket;
import com.deskflow.entity.TicketStatus;
import com.deskflow.entity.User;
import com.deskflow.exception.BusinessRuleException;
import com.deskflow.exception.ForbiddenOperationException;
import com.deskflow.mapper.CommentMapper;
import com.deskflow.mapper.UserMapper;
import com.deskflow.repository.CommentRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserService userService;

    private CommentService commentService;

    private final User requester = TestFixtures.requester(4L);
    private final User otherRequester = TestFixtures.requester(5L);

    @BeforeEach
    void setUp() {
        commentService = new CommentService(
                commentRepository,
                ticketRepository,
                userService,
                new TicketAccessPolicy(new TicketStatusTransitionPolicy()),
                new CommentMapper(new UserMapper()),
                new PageableSanitizer()
        );
    }

    @Test
    void deveAdicionarComentarioEmChamadoAcessivel() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));
        when(userService.getActiveUser(4L)).thenReturn(requester);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 50L);
            return comment;
        });

        CommentResponse response = commentService.add(
                100L,
                new CreateCommentRequest("  Já tentei reiniciar o equipamento.  "),
                TestFixtures.principal(requester)
        );

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.ticketId()).isEqualTo(100L);
        assertThat(response.author().id()).isEqualTo(4L);
        assertThat(response.content()).isEqualTo("Já tentei reiniciar o equipamento.");
    }

    @Test
    void naoDeveComentarEmChamadoEncerrado() {
        Ticket ticket = TestFixtures.ticketWithStatus(
                100L, requester, TicketStatus.CLOSED
        );
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> commentService.add(
                100L,
                new CreateCommentRequest("Comentário após encerramento."),
                TestFixtures.principal(requester)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void naoDeveComentarSemPermissao() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> commentService.add(
                100L,
                new CreateCommentRequest("Tentativa indevida."),
                TestFixtures.principal(otherRequester)
        )).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deveListarComentariosDoChamado() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        Comment comment = new Comment(ticket, requester, "Primeiro comentário.");
        Page<Comment> page = new PageImpl<>(
                List.of(comment),
                PageRequest.of(0, 20),
                1
        );
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicketId(eq(100L), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<CommentResponse> response = commentService.list(
                100L,
                PageRequest.of(0, 20),
                TestFixtures.principal(requester)
        );

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().getFirst().content()).isEqualTo("Primeiro comentário.");
    }

    @Test
    void naoDeveListarComentariosSemPermissao() {
        Ticket ticket = TestFixtures.ticket(100L, requester);
        when(ticketRepository.findDetailedById(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> commentService.list(
                100L,
                PageRequest.of(0, 20),
                TestFixtures.principal(otherRequester)
        )).isInstanceOf(ForbiddenOperationException.class);
    }
}
