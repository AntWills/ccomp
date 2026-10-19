package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.domain.events.guests.external.GuestInvitationMessageDTO;
import com.ccomp.br.domain.events.guests.external.message.GuestInvitationChannel;
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
public class GuestInvitationConsumer {
    @Value("${app.frontend.accept-guest-invite-url}")
    private String acceptGuestInviteUrl;

    private final EmailService emailService;
    private final EmailTemplateService templateService;

    public GuestInvitationConsumer(EmailService emailService, EmailTemplateService templateService) {
        this.emailService = emailService;
        this.templateService = templateService;
    }

    @RabbitListener(queues = GuestInvitationChannel.QUEUE)
    public void onGuestInvitation(GuestInvitationMessageDTO invitation) {
        String subject = "[CONVITE] Convidado para: " + invitation.eventTitle();
        String invitationUrl = acceptGuestInviteUrl + invitation.code();
        String htmlContent = templateService.render(
                "guest-invitation",
                subject,
                Map.of(
                        "eventTitle", invitation.eventTitle(),
                        "invitationUrl", invitationUrl
                )
        );

        emailService.send(new SendMailDTO(
                invitation.emailAddress(),
                subject,
                htmlContent,
                true
        ));
        log.info("Convite de evento enviado para {}", invitation.emailAddress().getValue());
    }
}
