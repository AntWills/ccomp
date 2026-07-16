package com.ccomp.br.config.dev;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
@Profile("!prod")
public class MailDebugConfig {
    @Value("${spring.mail.host:NAO_DEFINIDO}")
    private String mailHost;

    @Value("${spring.mail.port:NAO_DEFINIDO}")
    private String mailPort;

    @Value("${spring.mail.username:NAO_DEFINIDO}")
    private String mailUsername;

    @Value("${spring.mail.password:NAO_DEFINIDO}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:NAO_DEFINIDO}")
    private String smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:NAO_DEFINIDO}")
    private String starttlsEnable;

    @Bean
    public CommandLineRunner mailDebugRunner() {
        return args -> {
            log.info("============ CONFIGURAÇÕES DE MAIL ============");
            log.info("spring.mail.host = {}", mailHost);
            log.info("spring.mail.port = {}", mailPort);
            log.info("spring.mail.username = {}", maskValue(mailUsername));
            log.info("spring.mail.password = {}", maskValue(mailPassword));
            log.info("spring.mail.properties.mail.smtp.auth = {}", smtpAuth);
            log.info("spring.mail.properties.mail.smtp.starttls.enable = {}", starttlsEnable);
            log.info("================================================");

            // Verificação adicional
            verificarVariaveisAmbiente();
        };
    }

    private String maskValue(String value) {
        if (value == null || value.equals("NAO_DEFINIDO")) {
            return value;
        }
        return value.length() > 4
                ? "****" + value.substring(value.length() - 4)
                : "****";
    }

    private void verificarVariaveisAmbiente() {
        log.info("----- Variáveis de Ambiente -----");
        log.info("MAIL_HOST (env) = {}", System.getenv("MAIL_HOST"));
        log.info("MAIL_PORT (env) = {}", System.getenv("MAIL_PORT"));
        log.info("MAIL_USERNAME (env) = {}", maskValue(System.getenv("MAIL_USERNAME")));
        log.info("MAIL_PASSWORD (env) = {}", maskValue(System.getenv("MAIL_PASSWORD")));
        log.info("MAIL_SMTP_AUTH (env) = {}", System.getenv("MAIL_SMTP_AUTH"));
        log.info("MAIL_SMTP_STARTTLS_ENABLE (env) = {}", System.getenv("MAIL_SMTP_STARTTLS_ENABLE"));
    }
}
