package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.domain.events.instructor.external.InstructorInvitationMessageDTO;
import com.ccomp.br.domain.events.instructor.external.message.InstructorInvitationChannel;
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
public class InstructorInvitationConsumer {
    @Value("${app.frontend.accept-instructor-invite-url}")
    private String acceptInstructorInviteUrl;

    private final EmailService emailService;
    private final EmailTemplateService templateService;

    public InstructorInvitationConsumer(
            EmailService emailService,
            EmailTemplateService templateService
    ) {
        this.emailService = emailService;
        this.templateService = templateService;
    }

    @RabbitListener(queues = InstructorInvitationChannel.QUEUE)
    public void onInstructorInvitation(InstructorInvitationMessageDTO invitation) {
        String subject = "[CONVITE] Ministrante para: " + invitation.activityTitle();
        String acceptUrl = acceptInstructorInviteUrl + invitation.code();
        String htmlContent = templateService.render(
                "instructor-invitation",
                subject,
                Map.of(
                        "eventTitle", invitation.eventTitle(),
                        "activityTitle", invitation.activityTitle(),
                        "acceptUrl", acceptUrl
                )
        );

        emailService.send(new SendMailDTO(
                invitation.emailAddress(),
                subject,
                htmlContent,
                true
        ));
        log.info("Convite para ministrar atividade enviado para {}", invitation.emailAddress());
    }
}
