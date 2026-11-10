package com.ccomp.br.domain.events.notifications.email;

import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.shared.dto.SendMailDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class EventNotificationEmailService {
    private final EmailService emailService;
    private final String notificationTemplate;
    private final String layoutTemplate;

    public EventNotificationEmailService(EmailService emailService) {
        this.emailService = emailService;
        this.notificationTemplate = readTemplate("templates/email/event-notification.html");
        this.layoutTemplate = readTemplate("templates/email/layout.html");
    }

    public void send(String email, String recipientName, String eventTitle, String title, String message) {
        String html = render(recipientName, eventTitle, title, message);
        emailService.send(new SendMailDTO(new EmailAddress(email), title, html, true));
    }

    String render(String recipientName, String eventTitle, String title, String message) {
        String template = notificationTemplate;
        Map<String, String> values = Map.of(
                "recipientName", recipientName,
                "eventTitle", eventTitle,
                "notificationTitle", title,
                "message", message);
        for (var value : values.entrySet()) {
            template = template.replace("{{" + value.getKey() + "}}", escapeHtml(value.getValue()));
        }
        return layoutTemplate
                .replace("{{title}}", escapeHtml(title))
                .replace("{{body}}", template);
    }

    private String readTemplate(String path) {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível carregar o template de notificação por email.", exception);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
