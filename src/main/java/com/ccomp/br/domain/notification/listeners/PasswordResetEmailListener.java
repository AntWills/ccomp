package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.config.RabbitMQConfig;
import com.ccomp.br.domain.auth.core.external.dto.PasswordResetMessageDTO;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.module.email.EmailTemplateService;
import com.ccomp.br.shared.dto.SendMailDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PasswordResetEmailListener {
    @Value("${app.frontend.password-reset-url}")
    private String frontendResetUrl;
    private final EmailService emailService;
    private final EmailTemplateService templateService;

    public PasswordResetEmailListener(EmailService emailService, EmailTemplateService templateService) {
        this.emailService = emailService;
        this.templateService = templateService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PASSWORD_RESET)
    public void handler(PasswordResetMessageDTO event) {
        // Complete os demais passos.
        String subject = "Redefinir Senha da plataforma CCOMP";
        String resetLink = frontendResetUrl + "?token=" + event.token();
        String body = templateService.render("password-reset", subject, Map.of("resetUrl", resetLink));

        SendMailDTO dto = new SendMailDTO(
                event.email(),
                subject,
                body,
                true
        );

        emailService.send(dto);
    }

}
