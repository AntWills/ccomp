package com.ccomp.br.domain.events.notifications.application;

import com.ccomp.br.module.email.EmailService;
import com.ccomp.br.domain.events.notifications.email.EventNotificationEmailService;
import com.ccomp.br.shared.dto.SendMailDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventNotificationEmailServiceTest {
    @Mock EmailService emailService;
    @InjectMocks EventNotificationEmailService notificationEmailService;

    @Test
    void loadsDomainTemplateEscapesContentAndUsesEmailTransport() {
        notificationEmailService.send("ana@example.org", "Ana <A>", "DevConf & Fórum",
                "Mudança", "Sala <A> & B");

        ArgumentCaptor<SendMailDTO> mail = ArgumentCaptor.forClass(SendMailDTO.class);
        verify(emailService).send(mail.capture());
        assertThat(mail.getValue().to().getValue()).isEqualTo("ana@example.org");
        assertThat(mail.getValue().subject()).isEqualTo("Mudança");
        assertThat(mail.getValue().isHtml()).isTrue();
        assertThat(mail.getValue().text()).contains("Ana &lt;A&gt;", "DevConf &amp; Fórum", "Sala &lt;A&gt; &amp; B");
    }
}
