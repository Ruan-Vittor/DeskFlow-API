package com.deskflow.service;

import com.deskflow.dto.auth.AuthResponse;
import com.deskflow.dto.auth.LoginRequest;
import com.deskflow.dto.auth.RegisterRequest;
import com.deskflow.dto.user.UserResponse;
import com.deskflow.entity.User;
import com.deskflow.exception.InvalidCredentialsException;
import com.deskflow.mapper.UserMapper;
import com.deskflow.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(UserService userService, AuthenticationManager authenticationManager,
                       JwtService jwtService, UserMapper userMapper) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserResponse registered = userService.registerRequester(request);
        User user = userService.getByEmail(registered.email());
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = userService.normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        User user = userService.getByEmail(email);
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(
                token,
                jwtService.getExpirationSeconds(),
                userMapper.toResponse(user)
        );
    }
}
