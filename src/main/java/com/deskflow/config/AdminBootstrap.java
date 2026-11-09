package com.deskflow.config;

import com.deskflow.entity.Role;
import com.deskflow.entity.User;
import com.deskflow.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(BootstrapAdminProperties properties, UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.email()) && !StringUtils.hasText(properties.password())) {
            return;
        }
        if (!StringUtils.hasText(properties.email())) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_EMAIL deve ser informado quando BOOTSTRAP_ADMIN_PASSWORD está definido."
            );
        }
        if (!StringUtils.hasText(properties.password())) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD deve ser informado quando BOOTSTRAP_ADMIN_EMAIL está definido."
            );
        }
        if (properties.password().length() < 8 || properties.password().length() > 72) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD deve ter entre 8 e 72 caracteres."
            );
        }

        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Usuário administrador inicial já existe: {}", email);
            return;
        }

        String name = StringUtils.hasText(properties.name())
                ? properties.name().trim()
                : "Administrador";
        User admin = new User(name, email, passwordEncoder.encode(properties.password()), Role.ADMIN);
        userRepository.save(admin);

        log.info("Usuário administrador inicial criado: {}", email);
    }
}
