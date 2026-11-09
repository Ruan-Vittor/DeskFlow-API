package com.deskflow.controller;

import com.deskflow.dto.common.ApiResponse;
import com.deskflow.dto.common.PageResponse;
import com.deskflow.dto.user.CreateUserRequest;
import com.deskflow.dto.user.UpdateProfileRequest;
import com.deskflow.dto.user.UserResponse;
import com.deskflow.security.UserPrincipal;
import com.deskflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários", description = "Perfil do usuário e administração de contas")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuários (somente ADMIN)")
    public ApiResponse<PageResponse<UserResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success("Usuários listados com sucesso.", userService.listUsers(pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar usuário com perfil específico (somente ADMIN)",
            description = "Permite criar ADMIN, TECHNICIAN ou REQUESTER. O cadastro público não permite escolher o perfil."
    )
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("Usuário criado com sucesso.", userService.createUser(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar o próprio perfil")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                "Perfil consultado com sucesso.",
                userService.getProfile(principal.id())
        );
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar o próprio perfil")
    public ApiResponse<UserResponse> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(
                "Perfil atualizado com sucesso.",
                userService.updateProfile(principal.id(), request)
        );
    }
}
