package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.config.RabbitMQConfig;
import com.ccomp.br.domain.users.external.dto.UserCreatedMessageDTO;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.module.email.EmailTemplateService;
import com.ccomp.br.shared.dto.SendMailDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserCreatedConsumer {
    private final EmailService emailService;
    private final EmailTemplateService templateService;

    public UserCreatedConsumer(EmailService emailService, EmailTemplateService templateService) {
        this.emailService = emailService;
        this.templateService = templateService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_CREATED)
    public void handler(UserCreatedMessageDTO message){
        String subject = "Bem-vindo à CCOMP";
        String body = templateService.render("welcome", subject, Map.of("name", message.name()));

        SendMailDTO dto = new SendMailDTO(
                new EmailAddress(message.email()),
                subject,
                body,
                true
        );

        emailService.send(dto);
    }

}
