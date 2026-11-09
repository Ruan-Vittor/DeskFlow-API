package com.deskflow.service.support;

import com.deskflow.exception.BusinessRuleException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageableSanitizerTest {

    private final PageableSanitizer sanitizer = new PageableSanitizer();

    @Test
    void deveAplicarOrdenacaoPadraoDeChamados() {
        Pageable result = sanitizer.forTickets(PageRequest.of(0, 20));

        assertThat(result.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(result.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(result.getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void deveAplicarOrdenacaoPadraoDeComentarios() {
        Pageable result = sanitizer.forComments(PageRequest.of(0, 20));

        assertThat(result.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(result.getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void deveRespeitarOrdenacaoValida() {
        Pageable result = sanitizer.forTickets(
                PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "title"))
        );

        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getSort().getOrderFor("title").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void deveRejeitarCampoDeOrdenacaoDesconhecido() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("senha"));

        assertThatThrownBy(() -> sanitizer.forTickets(pageable))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("senha");
    }

    @Test
    void deveLimitarTamanhoMaximoDaPagina() {
        Pageable result = sanitizer.forUsers(PageRequest.of(0, 500));

        assertThat(result.getPageSize()).isEqualTo(100);
    }

    @Test
    void deveAceitarTodosOsCamposPermitidosDeChamados() {
        List<String> fields = List.of(
                "id", "title", "status", "priority", "category", "createdAt", "updatedAt"
        );

        fields.forEach(field -> assertThat(sanitizer.forTickets(
                PageRequest.of(0, 10, Sort.by(field))
        ).getSort().getOrderFor(field)).isNotNull());
    }
}
