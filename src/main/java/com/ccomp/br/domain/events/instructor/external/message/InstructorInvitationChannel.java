package com.ccomp.br.domain.events.instructor.external.message;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InstructorInvitationChannel {
    private static final String NAME = "instructor-invitation";
    private static final String ROUTING_KEY = "instructor.invitation";

    public static final String QUEUE = MessagingChannel.PREFIX + NAME + ".queue";
    public static final String ROUTING = ROUTING_KEY;

    @Bean
    MessagingChannel instructorInvitation() {
        return new MessagingChannel(NAME, ROUTING_KEY);
    }
}
