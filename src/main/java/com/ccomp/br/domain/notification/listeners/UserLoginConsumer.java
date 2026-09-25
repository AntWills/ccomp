package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.config.RabbitMQConfig;
import com.ccomp.br.domain.auth.core.external.dto.UserLoginMessageDTO;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.module.email.EmailTemplateService;
import com.ccomp.br.shared.dto.SendMailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
public class UserLoginConsumer {
    @Value("${app.frontend.security-settings-url}")
    private String securitySettingsUrl;

    private final EmailService emailService;
    private final EmailTemplateService templateService;

    public UserLoginConsumer(EmailService emailService, EmailTemplateService templateService) {
        this.emailService = emailService;
        this.templateService = templateService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_LOGIN)
    public void onUserLogin(UserLoginMessageDTO event) {

        String subject = "[SEGURANÇA] Novo acesso detectado na sua conta";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss");
        String formattedDate = event.timestamp() != null
                ? event.timestamp().format(formatter)
                : "Data/hora não informada";
        String ipAddress = event.ipAddress() != null && !event.ipAddress().isBlank()
                ? event.ipAddress() : "Não identificado";
        String userAgent = event.userAgent() != null && !event.userAgent().isBlank()
                ? event.userAgent() : "Não identificado";
        String htmlContent = templateService.render("new-login", subject, Map.of(
                "timestamp", formattedDate,
                "ipAddress", ipAddress,
                "userAgent", userAgent,
                "securityUrl", securitySettingsUrl
        ));

        emailService.send(new SendMailDTO(event.email(), subject, htmlContent, true));
        log.info("E-mail de notificação de login enviado com sucesso para {}", event.email());
    }
}