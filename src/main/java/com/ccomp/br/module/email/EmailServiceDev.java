package com.ccomp.br.module.email;

import com.ccomp.br.shared.dto.SendMailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
@Primary
@Slf4j
public class EmailServiceDev extends EmailService {
    public EmailServiceDev(JavaMailSender mailSender) {
        super(mailSender);
    }

    @Override
    public void send(SendMailDTO dto) {
        log.info("[DEV] Email NÃO enviado → Para: {} | Assunto: {}",
                dto.to().getValue(), dto.subject());
    }
}
