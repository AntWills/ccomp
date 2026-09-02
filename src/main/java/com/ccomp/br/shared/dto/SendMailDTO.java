package com.ccomp.br.shared.dto;

import com.ccomp.br.module.email.EmailAddress;

public record SendMailDTO(
        EmailAddress to,
        String subject,
        String text,
        boolean isHtml
) {
    public SendMailDTO(EmailAddress to, String subject, String body) {
        this(to, subject, body, false);
    }
}
