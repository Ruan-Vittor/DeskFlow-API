package com.deskflow.security;

import com.deskflow.config.JwtProperties;
import com.deskflow.entity.User;
import com.deskflow.support.TestFixtures;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "unit-test-secret-key-with-at-least-thirty-two-bytes-123456";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 60L));
    }

    @Test
    void deveGerarTokenComSubjectPerfilEIdentificador() {
        User user = TestFixtures.technician(7L);

        String token = jwtService.generateToken(user);
        Claims claims = jwtService.parseClaims(token);

        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo(user.getEmail());
        assertThat(claims.get("role", String.class)).isEqualTo("TECHNICIAN");
        assertThat(((Number) claims.get("uid")).longValue()).isEqualTo(7L);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration().toInstant()).isAfter(Instant.now());
    }

    @Test
    void deveExporTempoDeExpiracaoEmSegundos() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600L);
    }

    @Test
    void deveRejeitarTokenInvalido() {
        assertThatThrownBy(() -> jwtService.parseClaims("token-invalido"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarSegredoMenorQueTrintaEDoisBytes() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("segredo-curto", 60L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void deveRejeitarExpiracaoInvalida() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(SECRET, 0L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_EXPIRATION_MINUTES");
    }
}
