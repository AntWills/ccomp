package com.ccomp.br.domain.users.external.message;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserLoginChannel {
    private static final String NAME = "user-login";
    private static final String ROUTING_KEY = "user.login";

    // Expostas como constantes para uso em @RabbitListener e publishers
    public static final String QUEUE = MessagingChannel.PREFIX + NAME + ".queue";
    public static final String ROUTING = ROUTING_KEY;

    @Bean
    MessagingChannel userLogin() {
        return new MessagingChannel(NAME, ROUTING_KEY);
    }
}
