package com.ccomp.br.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "ccomp.events.exchange";

    // --- 1. PROCESSO: CRIAÇÃO DE USUÁRIO (E-mail) ---
    public static final String QUEUE_USER_CREATED = "ccomp.user-created.queue";
    public static final String ROUTING_KEY_USER_CREATED = "user.created";

    public static final String DLQ_USER_CREATED = "ccomp.user-created.dlq";
    public static final String DLQ_ROUTING_KEY_USER_CREATED = "user.created.dlq";

    // --- 1. PROCESSO: ENVIO DE CONVITE PARA SER EDITOR (E-mail) ---
    public static final String QUEUE_EDITOR_INVITATION = "ccomp.editor-invitation.queue";
    public static final String ROUTING_KEY_EDITOR_INVITATION = "editor.invitation";

    public static final String DLQ_EDITOR_INVITATION = "ccomp.editor-invitation.dlq";
    public static final String DLQ_ROUTING_KEY_EDITOR_INVITATION = "editor.invitation.dlq";

    // --- 2. PROCESSO: GERAÇÃO DE CERTIFICADO ---
    public static final String QUEUE_CERTIFICATE = "ccomp.certificate-generate.queue";
    public static final String ROUTING_KEY_CERTIFICATE = "certificate.generate";

    public static final String DLQ_CERTIFICATE = "ccomp.certificate-generate.dlq";
    public static final String DLQ_ROUTING_KEY_CERTIFICATE = "certificate.generate.dlq";

    // --- EXCHANGE PRINCIPAL ---
    @Bean
    public TopicExchange appExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    // ==========================================
    // CONFIGURAÇÃO: USER CREATED
    // ==========================================

    @Bean
    public Queue userCreatedQueue() {
        Map<String, Object> args = new HashMap<>();
        // Redireciona falhas para a Exchange principal usando a routing key da DLQ
        args.put("x-dead-letter-exchange", EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY_USER_CREATED);
        return QueueBuilder.durable(QUEUE_USER_CREATED).withArguments(args).build();
    }

    @Bean
    public Queue userCreatedDlq() {
        return QueueBuilder.durable(DLQ_USER_CREATED).build();
    }

    @Bean
    public Binding bindingUserCreated(Queue userCreatedQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(userCreatedQueue).to(appExchange).with(ROUTING_KEY_USER_CREATED);
    }

    @Bean
    public Binding bindingUserCreatedDlq(Queue userCreatedDlq, TopicExchange appExchange) {
        return BindingBuilder.bind(userCreatedDlq).to(appExchange).with(DLQ_ROUTING_KEY_USER_CREATED);
    }

    // ==========================================
    // CONFIGURAÇÃO: EDITOR INVITATION
    // ==========================================

    @Bean
    public Queue editorInvitationQueue() {
        Map<String, Object> args = new HashMap<>();
        // Redireciona falhas para a Exchange principal usando a routing key da DLQ
        args.put("x-dead-letter-exchange", EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY_EDITOR_INVITATION);
        return QueueBuilder.durable(QUEUE_EDITOR_INVITATION).withArguments(args).build();
    }

    @Bean
    public Queue editorInvitationDlq() {
        return QueueBuilder.durable(DLQ_EDITOR_INVITATION).build();
    }

    @Bean
    public Binding bindingEditorInvitation(Queue editorInvitationQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(editorInvitationQueue).to(appExchange).with(ROUTING_KEY_EDITOR_INVITATION);
    }

    @Bean
    public Binding bindingEditorInvitationDlq(Queue editorInvitationDlq, TopicExchange appExchange) {
        return BindingBuilder.bind(editorInvitationDlq).to(appExchange).with(DLQ_ROUTING_KEY_EDITOR_INVITATION);
    }

    // ==========================================
    // CONFIGURAÇÃO: CERTIFICADO
    // ==========================================

    @Bean
    public Queue certificateQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY_CERTIFICATE);
        return QueueBuilder.durable(QUEUE_CERTIFICATE).withArguments(args).build();
    }

    @Bean
    public Queue certificateDlq() {
        return QueueBuilder.durable(DLQ_CERTIFICATE).build();
    }

    @Bean
    public Binding bindingCertificate(Queue certificateQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(certificateQueue).to(appExchange).with(ROUTING_KEY_CERTIFICATE);
    }

    @Bean
    public Binding bindingCertificateDlq(Queue certificateDlq, TopicExchange appExchange) {
        return BindingBuilder.bind(certificateDlq).to(appExchange).with(DLQ_ROUTING_KEY_CERTIFICATE);
    }

    // ==========================================
    // INFRAESTRUTURA
    // ==========================================

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