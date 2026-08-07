package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.domain.auth.external.dto.PasswordResetRequestedEvent;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.shared.dto.SendMailDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetEmailListener {
    @Value("${app.frontend.password-reset-url}")
    private String frontendResetUrl;
    private final EmailService emailService;

    public PasswordResetEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void handler(PasswordResetRequestedEvent event) {
        // Complete os demais passos.
        String subject = "Redefinir Senha da plataforma CCOMP";
        String resetLink = frontendResetUrl + "?token=" + event.token();
        String body = buildBody(resetLink);

        SendMailDTO dto = new SendMailDTO(
                event.email(),
                subject,
                body
        );

        emailService.send(dto);
    }

    private String buildBody(String resetLink) {
        return  """
                Olá,

                Recebemos uma solicitação para redefinir a senha da sua conta.

                Para continuar, acesse o link abaixo:
                %s

                Este link é válido por 10 minutos. Se você não solicitou essa alteração, ignore este e-mail — sua senha permanecerá inalterada.

                Atenciosamente,
                Equipe ccomp
                """.formatted(resetLink);
    }
}
