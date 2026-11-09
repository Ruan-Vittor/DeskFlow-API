package com.deskflow.controller;

import com.deskflow.dto.comment.CommentResponse;
import com.deskflow.dto.comment.CreateCommentRequest;
import com.deskflow.dto.common.ApiResponse;
import com.deskflow.dto.common.PageResponse;
import com.deskflow.security.UserPrincipal;
import com.deskflow.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@Tag(name = "Comentários", description = "Interações nos chamados")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Adicionar comentário",
            description = "Somente usuários com acesso ao chamado podem comentar e chamados encerrados não aceitam comentários."
    )
    public ApiResponse<CommentResponse> add(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Comentário adicionado com sucesso.",
                commentService.add(ticketId, request, principal)
        );
    }

    @GetMapping
    @Operation(summary = "Listar comentários do chamado")
    public ApiResponse<PageResponse<CommentResponse>> list(
            @PathVariable Long ticketId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Comentários listados com sucesso.",
                commentService.list(ticketId, pageable, principal)
        );
    }
}
