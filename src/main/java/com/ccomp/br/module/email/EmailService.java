package com.ccomp.br.module.email;

import com.ccomp.br.shared.dto.SendMailDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(SendMailDTO dto){
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(dto.to().getValue());
            helper.setSubject(dto.subject());

            helper.setText(dto.text(), dto.isHtml());

            mailSender.send(message);
            log.info("E-mail enviado com sucesso para {}", dto.to().getValue());

        } catch (MessagingException | MailException e) {
            log.error("O envio de e-mail para {} não foi possível!", dto.to().getValue());
            log.error(e.getMessage(), e);
        }
    }
}
