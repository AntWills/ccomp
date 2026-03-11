package com.ccomp.br.module.email;

import com.ccomp.br.shared.dto.SendMailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(SendMailDTO dto){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(dto.to().getValue());
        message.setSubject(dto.subject());
        message.setText(dto.text());

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("O envio de email para {} não foi possivel!", dto.to().getValue());
            log.error(e.getMessage());
        }
    }
}
