package com.ccomp.br.shared.dto;

import java.util.Objects;

/** An in-memory file attachment for an email. */
public record MailAttachment(String filename, byte[] content, String contentType) {
    public MailAttachment {
        Objects.requireNonNull(filename, "filename must not be null");
        Objects.requireNonNull(content, "content must not be null");
        content = content.clone();
        contentType = contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
