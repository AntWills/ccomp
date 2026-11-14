package com.ccomp.br.module.email;

import com.ccomp.br.shared.dto.MailAttachment;
import com.ccomp.br.shared.dto.SendMailDTO;
import jakarta.mail.Session;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Properties;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {
    @Test
    void sendsMailWithArbitraryAttachmentsAndContentTypes() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        EmailService service = new EmailService(mailSender);

        service.send(new SendMailDTO(new EmailAddress("person@example.org"), "Files", "See attached", false,
                List.of(
                        new MailAttachment("certificate.png", new byte[]{1, 2, 3}, "image/png"),
                        new MailAttachment("certificate.pdf", new byte[]{4, 5, 6}, "application/pdf"),
                        new MailAttachment("archive.custom", new byte[]{7, 8}, null))));

        verify(mailSender).send(message);
        message.saveChanges();
        List<BodyPart> parts = new ArrayList<>();
        collectParts((Multipart) message.getContent(), parts);
        assertThat(parts).hasSize(4);
        assertThat(parts).anySatisfy(part -> {
            assertThat(part.getFileName()).isEqualTo("certificate.png");
            assertThat(part.getContentType()).startsWith("image/png");
        });
        assertThat(parts).anySatisfy(part -> {
            assertThat(part.getFileName()).isEqualTo("certificate.pdf");
            assertThat(part.getContentType()).startsWith("application/pdf");
        });
        assertThat(parts).anySatisfy(part -> {
            assertThat(part.getFileName()).isEqualTo("archive.custom");
            assertThat(part.getContentType()).startsWith("application/octet-stream");
        });
    }

    @Test
    void existingMailDtoConstructorsHaveNoAttachments() {
        var dto = new SendMailDTO(new EmailAddress("person@example.org"), "Subject", "Body", true);

        assertThat(dto.attachments()).isEmpty();
    }

    private void collectParts(Multipart multipart, List<BodyPart> parts) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object content = part.getContent();
            if (content instanceof Multipart nested) collectParts(nested, parts);
            else parts.add(part);
        }
    }
}
