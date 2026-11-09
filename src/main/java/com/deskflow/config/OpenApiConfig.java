package com.deskflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI deskFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DeskFlow API")
                        .description("""
                                API REST para gerenciamento de chamados de suporte técnico.

                                Perfis disponíveis:
                                - **ADMIN**: gerencia usuários e todos os chamados.
                                - **TECHNICIAN**: atende chamados disponíveis e atribuídos a si.
                                - **REQUESTER**: abre e acompanha seus próprios chamados.

                                Para testar endpoints protegidos, faça login em `POST /api/auth/login`
                                e informe o token no botão **Authorize** no formato `Bearer <token>`.
                                """)
                        .version("1.0.0"))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("Ambiente local")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
