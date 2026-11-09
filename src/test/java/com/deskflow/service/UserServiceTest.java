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
import com.deskflow.support.TestFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder,
                new UserMapper(),
                new PageableSanitizer()
        );
    }

    @Test
    void deveCadastrarRequesterComEmailNormalizadoESenhaCriptografada() {
        when(userRepository.existsByEmailIgnoreCase("novo@deskflow.test")).thenReturn(false);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("hash-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.registerRequester(
                new RegisterRequest(" Maria Silva ", " NOVO@DESKFLOW.TEST ", "senhaSegura123")
        );

        assertThat(response.name()).isEqualTo("Maria Silva");
        assertThat(response.email()).isEqualTo("novo@deskflow.test");
        assertThat(response.role()).isEqualTo(Role.REQUESTER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hash-bcrypt");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.REQUESTER);
    }

    @Test
    void deveRejeitarCadastroComEmailDuplicado() {
        when(userRepository.existsByEmailIgnoreCase("duplicado@deskflow.test")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerRequester(
                new RegisterRequest("Nome", "duplicado@deskflow.test", "senhaSegura123")
        )).isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void adminDeveCriarUsuarioComPerfilEscolhido() {
        when(userRepository.existsByEmailIgnoreCase("tech@deskflow.test")).thenReturn(false);
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("hash-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(new CreateUserRequest(
                "Técnico", "tech@deskflow.test", "senhaSegura123", Role.TECHNICIAN
        ));

        assertThat(response.role()).isEqualTo(Role.TECHNICIAN);
    }

    @Test
    void deveAtualizarNomeSemAlterarSenha() {
        User user = TestFixtures.requester(4L);
        when(userRepository.findByIdAndActiveTrue(4L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserResponse response = userService.updateProfile(
                4L,
                new UpdateProfileRequest("Nome Atualizado", null, null)
        );

        assertThat(response.name()).isEqualTo("Nome Atualizado");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void deveExigirSenhaAtualAoDefinirNovaSenha() {
        User user = TestFixtures.requester(4L);
        when(userRepository.findByIdAndActiveTrue(4L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateProfile(
                4L,
                new UpdateProfileRequest("Nome", null, "novaSenha123")
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("senha atual");

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void deveRejeitarSenhaAtualIncorreta() {
        User user = TestFixtures.requester(4L);
        when(userRepository.findByIdAndActiveTrue(4L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senhaErrada", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateProfile(
                4L,
                new UpdateProfileRequest("Nome", "senhaErrada", "novaSenha123")
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("incorreta");
    }

    @Test
    void deveAlterarSenhaComSenhaAtualCorreta() {
        User user = TestFixtures.requester(4L);
        when(userRepository.findByIdAndActiveTrue(4L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senhaAtual123", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("novo-hash");
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        userService.updateProfile(
                4L,
                new UpdateProfileRequest("Nome", "senhaAtual123", "novaSenha123")
        );

        assertThat(user.getPassword()).isEqualTo("novo-hash");
    }

    @Test
    void deveListarUsuariosComPaginacao() {
        User user = TestFixtures.requester(4L);
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<UserResponse> response = userService.listUsers(PageRequest.of(0, 10));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().email()).isEqualTo(user.getEmail());
    }

    @Test
    void deveRejeitarTecnicoInativoOuInexistente() {
        when(userRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getActiveTechnician(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveRejeitarUsuarioQueNaoPossuiPerfilTecnico() {
        User requester = TestFixtures.requester(4L);
        when(userRepository.findByIdAndActiveTrue(4L)).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> userService.getActiveTechnician(4L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("TECHNICIAN");
    }
}
