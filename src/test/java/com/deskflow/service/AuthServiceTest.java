package com.deskflow.service;

import com.deskflow.dto.auth.AuthResponse;
import com.deskflow.dto.auth.LoginRequest;
import com.deskflow.dto.auth.RegisterRequest;
import com.deskflow.dto.user.UserResponse;
import com.deskflow.entity.Role;
import com.deskflow.entity.User;
import com.deskflow.exception.InvalidCredentialsException;
import com.deskflow.mapper.UserMapper;
import com.deskflow.security.JwtService;
import com.deskflow.support.TestFixtures;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userService,
                authenticationManager,
                jwtService,
                new UserMapper()
        );
    }

    @Test
    void deveRegistrarUsuarioERetornarToken() {
        User user = TestFixtures.requester(4L);
        UserResponse registered = new UserResponse(
                user.getId(), user.getName(), user.getEmail(), Role.REQUESTER, true, Instant.now()
        );

        when(userService.registerRequester(any(RegisterRequest.class))).thenReturn(registered);
        when(userService.getByEmail(user.getEmail())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(
                new RegisterRequest("Solicitante", user.getEmail(), "senhaSegura123")
        );

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user().role()).isEqualTo(Role.REQUESTER);
    }

    @Test
    void deveAutenticarComCredenciaisValidas() {
        User user = TestFixtures.requester(4L);
        when(userService.normalizeEmail(" REQUESTER4@DESKFLOW.TEST "))
                .thenReturn("requester4@deskflow.test");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(null);
        when(userService.getByEmail("requester4@deskflow.test")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(
                new LoginRequest(" REQUESTER4@DESKFLOW.TEST ", "senhaSegura123")
        );

        assertThat(response.token()).isEqualTo("jwt-token");

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) captor.getValue();
        assertThat(authentication.getPrincipal()).isEqualTo("requester4@deskflow.test");
    }

    @Test
    void deveRejeitarCredenciaisInvalidas() {
        when(userService.normalizeEmail("requester4@deskflow.test"))
                .thenReturn("requester4@deskflow.test");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("requester4@deskflow.test", "senhaErrada")
        )).isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("E-mail ou senha inválidos.");
    }
}
