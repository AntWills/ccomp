package com.ccomp.br.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                        .description(apiDescription)
                        .termsOfService(tosUri)
                        .contact(new Contact()
                                .name("Baeldung")
                                .email("user-apis@baeldung.com")
                                .url("https://www.baeldung.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))

                .servers(List.of(
                        new Server()
                                .url(apiServerUrl)
                                .description("Ambiente Ativo")
                ))
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
}