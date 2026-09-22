package com.ccomp.br.domain.notification.listeners;

import com.ccomp.br.config.RabbitMQConfig;
import com.ccomp.br.domain.auth.core.external.dto.UserLoginMessageDTO;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.shared.dto.SendMailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class UserLoginConsumer {
    @Value("${app.frontend.security-settings-url}")
    private String securitySettingsUrl;

    private final EmailService emailService;

    public UserLoginConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_LOGIN)
    public void onUserLogin(UserLoginMessageDTO event) {

        String subject = "[SEGURANÇA] Novo acesso detectado na sua conta";

        String logoUrl = "https://cdn-icons-png.flaticon.com/512/888/888879.png";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss");
        String formattedDate = event.timestamp() != null
                ? event.timestamp().format(formatter)
                : "Data/Hora não informada";

        String ipAddress = (event.ipAddress() != null && !event.ipAddress().isBlank())
                ? event.ipAddress()
                : "Não identificado";

        String userAgent = (event.userAgent() != null && !event.userAgent().isBlank())
                ? event.userAgent()
                : "Não identificado";

        // HTML robusto para e-mails (com CSS inline e escapes %% para String.format)
        String htmlContent = String.format(
                "<!DOCTYPE html>" +
                        "<html lang='pt-BR'>" +
                        "<head>" +
                        "    <meta charset='UTF-8'>" +
                        "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "    <style>" +
                        "        body, table, td, a { -webkit-text-size-adjust: 100%%; -ms-text-size-adjust: 100%%; }" +
                        "        table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }" +
                        "        img { -ms-interpolation-mode: bicubic; border: 0; height: auto; line-height: 100%%; outline: none; text-decoration: none; }" +
                        "    </style>" +
                        "</head>" +
                        "<body style='margin: 0; padding: 0; background-color: #f4f4f4; font-family: Arial, sans-serif;'>" +
                        "    <table border='0' cellpadding='0' cellspacing='0' width='100%%'>" +
                        "        <tr>" +
                        "            <td align='center' style='padding: 20px 0;'>" +
                        "                <!-- Contêiner Principal -->" +
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
                        "                                        <h1 style='margin: 0; font-size: 18px; color: #000000; font-weight: bold;'>Novo acesso à sua conta</h1>" +
                        "                                        <p style='margin: 10px 0 0; font-size: 14px; color: #333333;'>Olá!</p>" +
                        "                                        <p style='margin: 5px 0 0; font-size: 14px; color: #555555;'>Detectamos um novo login realizado em sua conta:</p>" +
                        "                                        " +
                        "                                        <!-- Bloco de detalhes do login -->" +
                        "                                        <div style='margin-top: 15px; padding: 12px; background-color: #f8f9fa; border-left: 4px solid #0047AB; border-radius: 4px; font-size: 13px; color: #444444;'>" +
                        "                                            <p style='margin: 0 0 6px 0;'><strong>Data e Hora:</strong> %s</p>" +
                        "                                            <p style='margin: 0 0 6px 0;'><strong>Endereço IP:</strong> %s</p>" +
                        "                                            <p style='margin: 0;'><strong>Navegador / Dispositivo:</strong> %s</p>" +
                        "                                        </div>" +
                        "                                    </td>" +
                        "                                </tr>" +
                        "                            </table>" +
                        "                        </td>" +
                        "                    </tr>" +
                        "                    <tr>" +
                        "                        <td align='center' style='padding: 0 30px 30px; font-size: 12px; color: #888888;'>" +
                        "                            <div style='background-color: #fff3cd; color: #856404; padding: 10px; border-radius: 6px; font-size: 12px; text-align: left;'>" +
                        "                                <strong>Não foi você?</strong> Se você não reconhece este acesso, altere sua senha imediatamente para proteger sua conta." +
                        "                            </div>" +
                        "                        </td>" +
                        "                    </tr>" +
                        "                </table>" +
                        "                " +
                        "                <!-- Botão de Ação Centralizado -->" +
                        "                <table border='0' cellpadding='0' cellspacing='0' width='600' style='margin-top: 20px;'>" +
                        "                    <tr>" +
                        "                        <td align='center'>" +
                        "                            <a href='%s' style='display: inline-block; padding: 12px 24px; background-color: #d9534f; color: #ffffff; text-decoration: none; font-size: 15px; font-weight: bold; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.2);'>GERENCIAR SEGURANÇA</a>" +
                        "                        </td>" +
                        "                    </tr>" +
                        "                </table>" +
                        "            </td>" +
                        "        </tr>" +
                        "    </table>" +
                        "</body>" +
                        "</html>",
                logoUrl,
                formattedDate,
                ipAddress,
                userAgent,
                securitySettingsUrl
        );

        emailService.send(new SendMailDTO(event.email(), subject, htmlContent, true));
        log.info("E-mail de notificação de login enviado com sucesso para {}", event.email());
    }
}