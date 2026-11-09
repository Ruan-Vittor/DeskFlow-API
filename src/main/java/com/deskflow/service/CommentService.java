package com.deskflow.service;

import com.deskflow.dto.comment.CommentResponse;
import com.deskflow.dto.comment.CreateCommentRequest;
import com.deskflow.dto.common.PageResponse;
import com.deskflow.entity.Comment;
import com.deskflow.entity.Ticket;
import com.deskflow.entity.User;
import com.deskflow.exception.ResourceNotFoundException;
import com.deskflow.mapper.CommentMapper;
import com.deskflow.repository.CommentRepository;
import com.deskflow.repository.TicketRepository;
import com.deskflow.security.UserPrincipal;
import com.deskflow.service.support.PageableSanitizer;
import com.deskflow.service.support.TicketAccessPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final TicketAccessPolicy accessPolicy;
    private final CommentMapper commentMapper;
    private final PageableSanitizer pageableSanitizer;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository,
                          UserService userService, TicketAccessPolicy accessPolicy,
                          CommentMapper commentMapper, PageableSanitizer pageableSanitizer) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
        this.commentMapper = commentMapper;
        this.pageableSanitizer = pageableSanitizer;
    }

    @Transactional
    public CommentResponse add(Long ticketId, CreateCommentRequest request, UserPrincipal principal) {
        Ticket ticket = getTicket(ticketId);
        accessPolicy.assertCanComment(ticket, principal);

        User author = userService.getActiveUser(principal.id());
        Comment comment = new Comment(ticket, author, request.content().trim());
        commentRepository.save(comment);
        return commentMapper.toResponse(comment);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(Long ticketId, Pageable pageable,
                                               UserPrincipal principal) {
        Ticket ticket = getTicket(ticketId);
        accessPolicy.assertCanView(ticket, principal);

        Pageable safePageable = pageableSanitizer.forComments(pageable);
        Page<Comment> comments = commentRepository.findByTicketId(ticketId, safePageable);
        return PageResponse.from(comments.map(commentMapper::toResponse));
    }

    private Ticket getTicket(Long ticketId) {
        return ticketRepository.findDetailedById(ticketId)
                .orElseThrow(() -> ResourceNotFoundException.of("Chamado", ticketId));
    }
}
