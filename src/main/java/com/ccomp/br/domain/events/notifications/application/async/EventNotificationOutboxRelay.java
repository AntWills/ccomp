package com.ccomp.br.domain.events.notifications.application.async;

import com.ccomp.br.config.rabbit.RabbitMQConfig;
import com.ccomp.br.domain.events.notifications.external.message.*;
import com.ccomp.br.domain.events.notifications.persistence.EventNotificationOutbox;
import com.ccomp.br.domain.events.notifications.persistence.EventNotificationOutboxDslRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class EventNotificationOutboxRelay {
    private final EventNotificationOutboxDslRepository outboxDslRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    public EventNotificationOutboxRelay(EventNotificationOutboxDslRepository outboxDslRepository,
            @Qualifier("notificationRabbitTemplate") RabbitTemplate rabbitTemplate, Clock clock) {
        this.outboxDslRepository = outboxDslRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${notifications.outbox.poll-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        for (EventNotificationOutbox message : outboxDslRepository.findPendingForPublish()) {
            try {
                if ("DISPATCH".equals(message.getMessageType())) {
                    publishAndConfirm(EventNotificationDispatchChannel.ROUTING,
                            new EventNotificationDispatchMessage(message.getNotificationId()));
                } else if ("DELIVERY".equals(message.getMessageType())) {
                    var ids = Arrays.stream(message.getRecipientIds().split(","))
                            .map(Long::valueOf).toList();
                    publishAndConfirm(EventNotificationDeliveryChannel.ROUTING,
                            new EventNotificationDeliveryBatchMessage(message.getNotificationId(), ids));
                } else {
                    throw new IllegalStateException("Tipo de mensagem outbox desconhecido: " + message.getMessageType());
                }
                message.setAttempts(message.getAttempts() + 1);
                message.setPublishedAt(LocalDateTime.now(clock));
                message.setLastError(null);
            } catch (RuntimeException exception) {
                message.setAttempts(message.getAttempts() + 1);
                String error = exception.getMessage();
                message.setLastError(error == null ? exception.getClass().getSimpleName()
                        : error.substring(0, Math.min(error.length(), 4000)));
                log.error("Falha ao publicar mensagem outbox {} (notificação {})", message.getId(),
                        message.getNotificationId(), exception);
                break;
            }
        }
    }

    private void publishAndConfirm(String routingKey, Object payload) {
        CorrelationData correlation = new CorrelationData();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, payload, correlation);
        try {
            var confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck()) throw new IllegalStateException("RabbitMQ não confirmou a mensagem: " + confirm.getReason());
            if (correlation.getReturned() != null)
                throw new IllegalStateException("RabbitMQ não encontrou fila para a routing key " + routingKey);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Não foi possível confirmar a publicação no RabbitMQ.", exception);
        }
    }
}
