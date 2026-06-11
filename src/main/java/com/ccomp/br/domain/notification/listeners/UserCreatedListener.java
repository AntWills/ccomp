package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.domain.users.dto.UserCreatedEvent;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.shared.dto.SendMailDTO;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedListener {
    private final EmailService emailService;

    public UserCreatedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void handler(UserCreatedEvent event){
        String subject = "Bem-vindo à CComp - Sua conta foi criada com sucesso";
        String body =buildPlainTextWelcomeEmail(event.name());

        SendMailDTO dto = new SendMailDTO(
                event.emailAddress(),
                subject,
                body
        );

        emailService.send(dto);
    }

    private String buildPlainTextWelcomeEmail(String userName) {
        return String.format("""
        BEM-VINDO À CCOMP
        
        Olá %s,
        
        É com grande satisfação que confirmamos a criação da sua conta em nossa plataforma.
        Estamos muito felizes em tê-lo(a) conosco!
        
        Sua conta foi criada com sucesso! Agora você pode acessar todos os recursos
        e benefícios que preparamos para você.
        
        Próximos passos:
        - Complete seu perfil com informações adicionais
        - Explore nossos serviços e funcionalidades
        - Entre em contato conosco se precisar de suporte
        
        Acesse sua conta: https://www.ccomp.com.br/login
        
        Se você tiver alguma dúvida ou precisar de assistência, nossa equipe de suporte
        está à disposição para ajudá-lo(a).
        
        Atenciosamente,
        Equipe CComp
        
        ---
        📧 suporte@ccomp.com.br
        🌐 www.ccomp.com.br
        📍 Av. Principal, 1000 - Centro
        
        Este é um e-mail automático, por favor não responda diretamente a esta mensagem.
        """, userName);
    }
}
