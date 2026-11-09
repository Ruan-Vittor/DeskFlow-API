package com.deskflow.service;

import com.deskflow.dto.auth.RegisterRequest;
import com.deskflow.dto.common.PageResponse;
import com.deskflow.dto.user.CreateUserRequest;
import com.deskflow.dto.user.UpdateProfileRequest;
import com.deskflow.dto.user.UserResponse;
import com.deskflow.entity.Role;
import com.deskflow.entity.User;
import com.deskflow.exception.BusinessRuleException;
import com.deskflow.exception.DuplicateResourceException;
import com.deskflow.exception.ResourceNotFoundException;
import com.deskflow.mapper.UserMapper;
import com.deskflow.repository.UserRepository;
import com.deskflow.service.support.PageableSanitizer;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final PageableSanitizer pageableSanitizer;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       UserMapper userMapper, PageableSanitizer pageableSanitizer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.pageableSanitizer = pageableSanitizer;
    }

    @Transactional
    public UserResponse registerRequester(RegisterRequest request) {
        return createUser(request.name(), request.email(), request.password(), Role.REQUESTER);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        return createUser(request.name(), request.email(), request.password(), request.role());
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userMapper.toResponse(getActiveUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getActiveUser(userId);
        user.updateName(request.name().trim());

        if (StringUtils.hasText(request.newPassword())) {
            if (!StringUtils.hasText(request.currentPassword())) {
                throw new BusinessRuleException(
                        "A senha atual é obrigatória para alterar a senha."
                );
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new BusinessRuleException("A senha atual informada está incorreta.");
            }
            user.changePassword(passwordEncoder.encode(request.newPassword()));
        }

        userRepository.saveAndFlush(user);
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        Pageable safePageable = pageableSanitizer.forUsers(pageable);
        return PageResponse.from(
                userRepository.findAll(safePageable).map(userMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public User getActiveUser(Long userId) {
        return userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuário", userId));
    }

    @Transactional(readOnly = true)
    public User getActiveTechnician(Long technicianId) {
        User user = getActiveUser(technicianId);
        if (!user.isTechnician()) {
            throw new BusinessRuleException(
                    "O usuário informado não possui o perfil TECHNICIAN."
            );
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse createUser(String name, String email, String rawPassword, Role role) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Já existe um usuário com este e-mail.");
        }

        User user = new User(
                name.trim(),
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                role
        );
        userRepository.save(user);
        return userMapper.toResponse(user);
    }
}
