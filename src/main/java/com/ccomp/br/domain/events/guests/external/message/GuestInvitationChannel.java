package com.ccomp.br.domain.events.guests.external.message;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GuestInvitationChannel {
    private static final String NAME = "guest-invitation";
    private static final String ROUTING_KEY = "guest.invitation";

    public static final String QUEUE = MessagingChannel.PREFIX + NAME + ".queue";
    public static final String ROUTING = ROUTING_KEY;

    @Bean
    MessagingChannel guestInvitation() {
        return new MessagingChannel(NAME, ROUTING_KEY);
    }
}
