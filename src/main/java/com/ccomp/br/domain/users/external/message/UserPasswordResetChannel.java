package com.ccomp.br.domain.users.external.message;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserPasswordResetChannel {
    private static final String NAME = "password-reset";
    private static final String ROUTING_KEY = "password.reset";

    // Expostas como constantes para uso em @RabbitListener e publishers
    public static final String QUEUE = MessagingChannel.PREFIX + NAME + ".queue";
    public static final String ROUTING = ROUTING_KEY;

    @Bean
    MessagingChannel userPasswordReset() {
        return new MessagingChannel(NAME, ROUTING_KEY);
    }
}
