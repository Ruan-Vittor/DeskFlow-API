package com.deskflow.service.support;

import com.deskflow.entity.TicketStatus;
import com.deskflow.exception.InvalidStatusTransitionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStatusTransitionPolicyTest {

    private final TicketStatusTransitionPolicy policy = new TicketStatusTransitionPolicy();

    @ParameterizedTest
    @MethodSource("transicoesValidas")
    void devePermitirTransicoesValidas(TicketStatus current, TicketStatus target) {
        assertThatCode(() -> policy.assertValidTransition(current, target))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("transicoesInvalidas")
    void deveRejeitarTransicoesInvalidas(TicketStatus current, TicketStatus target) {
        assertThatThrownBy(() -> policy.assertValidTransition(current, target))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining(current.name())
                .hasMessageContaining(target.name());
    }

    @Test
    void devePermitirQueRequesterEncerreChamadoAberto() {
        assertThat(policy.isAllowedForRequester(TicketStatus.OPEN, TicketStatus.CLOSED)).isTrue();
    }

    @Test
    void devePermitirQueRequesterReabraChamadoResolvidoOuEncerrado() {
        assertThat(policy.isAllowedForRequester(TicketStatus.RESOLVED, TicketStatus.OPEN)).isTrue();
        assertThat(policy.isAllowedForRequester(TicketStatus.CLOSED, TicketStatus.OPEN)).isTrue();
    }

    @Test
    void requesterNaoDeveIniciarAtendimentoNemResolverChamado() {
        assertThat(policy.isAllowedForRequester(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)).isFalse();
        assertThat(policy.isAllowedForRequester(
                TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED)).isFalse();
    }

    private static Stream<Arguments> transicoesValidas() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_USER),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.WAITING_FOR_USER, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.WAITING_FOR_USER, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.WAITING_FOR_USER, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.OPEN),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.OPEN)
        );
    }

    private static Stream<Arguments> transicoesInvalidas() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.WAITING_FOR_USER),
                Arguments.of(TicketStatus.OPEN, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.OPEN, TicketStatus.OPEN),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.OPEN),
                Arguments.of(TicketStatus.WAITING_FOR_USER, TicketStatus.OPEN),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.RESOLVED)
        );
    }
}
