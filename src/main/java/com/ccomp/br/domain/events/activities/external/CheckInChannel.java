package com.ccomp.br.domain.events.activities.external;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CheckInChannel {
    private static final String NAME = "check-in";
    private static final String ROUTING_KEY = "check.in";

    // Expostas como constantes para uso em @RabbitListener e publishers
    public static final String QUEUE = MessagingChannel.PREFIX + NAME + ".queue";
    public static final String ROUTING = ROUTING_KEY;

    @Bean
    MessagingChannel checkIn() {
        return new MessagingChannel(NAME, ROUTING_KEY);
    }
}
