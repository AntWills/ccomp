package com.ccomp.br.domain.events.editors.external.message;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EditorInvitationChannel {
    private static final String NAME = "editor-invitation";
    private static final String ROUTING_KEY = "editor.invitation";

    // Expostas como constantes para uso em @RabbitListener e publishers
    public static final String QUEUE = MessagingChannel.PREFIX + NAME + ".queue";
    public static final String ROUTING = ROUTING_KEY;

    @Bean
    MessagingChannel editorInvitation() {
        return new MessagingChannel(NAME, ROUTING_KEY);
    }
}
