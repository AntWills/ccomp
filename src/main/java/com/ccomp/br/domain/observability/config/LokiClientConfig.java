package com.ccomp.br.domain.observability.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class LokiClientConfig {
    @Value("${app.observability.loki-url:http://grafana-lgtm:3100}")
    private String lokiUrl;

    @Bean
    public RestClient lokiRestClient() {
        return RestClient.builder()
                .baseUrl(lokiUrl)
                .build();
    }
}
