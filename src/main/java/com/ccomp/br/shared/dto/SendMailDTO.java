package com.ccomp.br.shared.dto;

import com.ccomp.br.module.email.EmailAddress;

import java.util.List;

public record SendMailDTO(
        EmailAddress to,
        String subject,
        String text,
        boolean isHtml,
        List<MailAttachment> attachments
) {
    public SendMailDTO {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public SendMailDTO(EmailAddress to, String subject, String text, boolean isHtml) {
        this(to, subject, text, isHtml, List.of());
    }

    public SendMailDTO(EmailAddress to, String subject, String body) {
        this(to, subject, body, false, List.of());
    }
}
