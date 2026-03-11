package com.ccomp.br.shared.dto;

import com.ccomp.br.module.email.EmailAddress;

public record SendMailDTO(
        EmailAddress to,
        String subject,
        String text
) {
}
