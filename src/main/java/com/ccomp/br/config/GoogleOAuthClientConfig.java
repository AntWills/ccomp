package com.ccomp.br.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.Assert;

@Configuration
@ConditionalOnProperty(name = "app.auth.google.enabled", havingValue = "true")
public class GoogleOAuthClientConfig {
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${CLIENT_ID:}") String clientId,
            @Value("${CLIENT_SECRET:}") String clientSecret) {
        Assert.hasText(clientId, "CLIENT_ID é obrigatório quando GOOGLE_OAUTH_ENABLED=true");
        Assert.hasText(clientSecret, "CLIENT_SECRET é obrigatório quando GOOGLE_OAUTH_ENABLED=true");

        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
