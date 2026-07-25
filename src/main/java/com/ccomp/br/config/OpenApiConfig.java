package com.ccomp.br.config;

import com.ccomp.br.domain.security.jwt.application.JwtService;
import com.ccomp.br.domain.security.UserDetailsImpl;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Value("${api.version:1.0.0}")
    private String apiVersion;

    @Value("${tos.uri:https://www.baeldung.com/terms}")
    private String tosUri;

    @Value("${api.description:Documentação das APIs de Usuários}")
    private String apiDescription;

    @Value("${api.server.url:http://localhost:8080}")
    private String apiServerUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("CCOMP API")
                        .version(apiVersion)
                        .description("Por favor, faça login no Swagger para gerar seu token de testes.")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira diretamente o seu token JWT gerado (sem digitar a palavra 'Bearer ')")));
    }

    @Bean
    public OpenApiCustomizer dynamicTokenOpenApiCustomizer(JwtService jwtService) {
        return openApi -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // Evita NullPointerException e garante que o usuário está realmente logado no seu formato
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) {
                return;
            }

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            String accessToken = jwtService.getAccessToken(userDetails.getId(), roles);
            // RefreshToken refreshToken = jwtService.getRefreshToken(userDetails.getId());

            // Descrição ultra direta e limpa, sem rodeios
            String dynamicDescription = """
                ### Quick-Pass JWT
                **Usuário:** %s
                
                ```text
                %s
                ```
                _Copie e cole no botão **Authorize** (cadeado)._
                """.formatted(auth.getName(), accessToken);

            openApi.getInfo().setDescription(dynamicDescription);
        };
    }
}