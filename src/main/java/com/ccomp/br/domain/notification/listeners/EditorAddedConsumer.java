package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.config.RabbitMQConfig;
import com.ccomp.br.domain.events.core.external.EditorAddedMessageDTO;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.module.email.EmailTemplateService;
import com.ccomp.br.shared.dto.SendMailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class EditorAddedConsumer {
    @Value("${app.frontend.accept-editor-invite-url}")
    private String frontendAcceptEditorInviteUrl;
    private final EmailService emailService;
    private final EmailTemplateService templateService;

    public EditorAddedConsumer(EmailService emailService, EmailTemplateService templateService) {
        this.emailService = emailService;
        this.templateService = templateService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EDITOR_INVITATION)
    public void onEditorAdded(EditorAddedMessageDTO event) {

        String subject = "[CONVITE] Editor para: " + event.eventTitle();

        String acceptUrl = frontendAcceptEditorInviteUrl + event.code();

        String htmlContent = templateService.render("event-invitation", subject, Map.of(
                "eventTitle", event.eventTitle(),
                "acceptUrl", acceptUrl
        ));

        emailService.send(new SendMailDTO(event.emailAddress(), subject, htmlContent, true));
        log.info("E-mail de convite (HTML) enviado para {} com o código {}", event.emailAddress().getValue(), event.code());
    }
}