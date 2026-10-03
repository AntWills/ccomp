package com.ccomp.br.config.rabbit;

import com.ccomp.br.shared.message.MessagingChannel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "ccomp.events.exchange";

    // --- 2. PROCESSO: ENVIO DE CONVITE PARA SER EDITOR (E-mail) ---
    public static final String QUEUE_EDITOR_INVITATION = "ccomp.editor-invitation.queue";
    public static final String ROUTING_KEY_EDITOR_INVITATION = "editor.invitation";

    // --- 4. PROCESSO: GERAÇÃO DE CERTIFICADO ---
    public static final String QUEUE_CERTIFICATE = "ccomp.certificate-generate.queue";
    public static final String ROUTING_KEY_CERTIFICATE = "certificate.generate";

    // --- EXCHANGE PRINCIPAL ---
    @Bean
    public TopicExchange appExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Declarables messagingDeclarables(List<MessagingChannel> channels, TopicExchange appExchange) {
        List<Declarable> declarables = new ArrayList<>();

        for (MessagingChannel channel : channels) {
            Queue queue = QueueBuilder.durable(channel.queue())
                    .deadLetterExchange(EXCHANGE_NAME)
                    .deadLetterRoutingKey(channel.dlqRoutingKey())
                    .build();
            Queue dlq = QueueBuilder.durable(channel.dlq()).build();

            declarables.add(queue);
            declarables.add(dlq);
            declarables.add(BindingBuilder.bind(queue).to(appExchange).with(channel.routingKey()));
            declarables.add(BindingBuilder.bind(dlq).to(appExchange).with(channel.dlqRoutingKey()));
        }
        return new Declarables(declarables);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}