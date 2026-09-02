package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.domain.events.external.dto.EditorAddedEvent;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.shared.dto.SendMailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class EditorAddedEventListener {
    private final EmailService emailService;

    public EditorAddedEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void onEditorAdded(EditorAddedEvent event) {

        String subject = "[CONVITE] Editor para: " + event.eventTitle();
//        String acceptUrl = "Ajustar Depois -> 8080/api/events/editors/accept?code=" + event.code();
        String acceptUrl = "https://example.com";
        // Logotipo simulado ou URL do logotipo real
        String logoUrl = "https://cdn-icons-png.flaticon.com/512/888/888879.png";

        // HTML robusto para e-mails (com CSS inline)
        String htmlContent = String.format(
                "<!DOCTYPE html>" +
                        "<html lang='pt-BR'>" +
                        "<head>" +
                        "    <meta charset='UTF-8'>" +
                        "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "    <style>" +
                        "        /* Estilos básicos para garantir renderização em clientes de e-mail */" +
                        "        body, table, td, a { -webkit-text-size-adjust: 100%%; -ms-text-size-adjust: 100%%; }" +
                        "        table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }" +
                        "        img { -ms-interpolation-mode: bicubic; border: 0; height: auto; line-height: 100%%; outline: none; text-decoration: none; }" +
                        "    </style>" +
                        "</head>" +
                        "<body style='margin: 0; padding: 0; background-color: #f4f4f4; font-family: Arial, sans-serif;'>" +
                        "    <table border='0' cellpadding='0' cellspacing='0' width='100%%'>" +
                        "        <tr>" +
                        "            <td align='center' style='padding: 20px 0;'>" +
                        "                <!-- Contêiner Principal (Post) -->" +
                        "                <table border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); overflow: hidden;'>" +
                        "                    <tr>" +
                        "                        <td style='padding: 30px;'>" +
                        "                            <table border='0' cellpadding='0' cellspacing='0' width='100%%'>" +
                        "                                <tr>" +
                        "                                    <!-- Logotipo e Textos -->" +
                        "                                    <td width='70' valign='top'>" +
                        "                                        <img src='%s' alt='CCOMP' width='60' height='60' style='display: block; border-radius: 50%%;'>" +
                        "                                    </td>" +
                        "                                    <td valign='top' style='padding-left: 20px;'>" +
                        "                                        <h1 style='margin: 0; font-size: 18px; color: #000000; font-weight: bold;'>[CONVITE] %s</h1>" +
                        "                                        <p style='margin: 10px 0 0; font-size: 14px; color: #333333;'>Olá, %s!</p>" +
                        "                                        <p style='margin: 5px 0 0; font-size: 14px; color: #555555;'>Você foi convidado para ser Editor no evento acima.</p>" +
                        "                                        <p style='margin: 5px 0 0; font-size: 12px; color: #777777; background-color: #fff9db; padding: 5px; border-radius: 4px; display: inline-block;'>Você tem até 24 horas para aceitar.</p>" +
                        "                                    </td>" +
                        "                                </tr>" +
                        "                            </table>" +
                        "                        </td>" +
                        "                    </tr>" +
                        "                    <tr>" +
                        "                        <td align='center' style='padding: 0 30px 30px; font-size: 12px; color: #888888;'>" +
                        "                            <img src='https://via.placeholder.com/16/888888/FFFFFF?text=?' alt='?' width='14' height='14' style='display: inline; vertical-align: middle; margin-right: 5px;'> " +
                        "                            Não esperava este convite? <a href='#' style='color: #888888; text-decoration: underline;'>Saiba mais.</a>" +
                        "                        </td>" +
                        "                    </tr>" +
                        "                </table>" +
                        "                <!-- Botão Centralizado (Fora do contêiner) -->" +
                        "                <table border='0' cellpadding='0' cellspacing='0' width='600' style='margin-top: 20px;'>" +
                        "                    <tr>" +
                        "                        <td align='center'>" +
                        "                            <a href='%s' style='display: inline-block; padding: 12px 24px; background-color: #0047AB; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: bold; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.2);'>ACEITAR CONVITE</a>" +
                        "                        </td>" +
                        "                    </tr>" +
                        "                </table>" +
                        "            </td>" +
                        "        </tr>" +
                        "    </table>" +
                        "</body>" +
                        "</html>",
                logoUrl,
                event.eventTitle(),
                event.editorUser().name(),
                acceptUrl
        );

        // Envia o e-mail como HTML (garanta que seu SendMailDTO e EmailService suportem isso)
        emailService.send(new SendMailDTO(event.editorUser().emailAddress(), subject, htmlContent, true));
        log.info("E-mail de convite (HTML) enviado para {} com o código {}", event.editorUser().emailAddress().getValue(), event.code());
    }
}