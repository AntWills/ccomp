package com.ccomp.br.domain.events.enrollments.listeners;

import com.ccomp.br.domain.events.activities.external.CheckInChannel;
import com.ccomp.br.domain.events.activities.external.CheckInMessageDTO;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.shared.exceptions.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class CheckInConsumer {
    private final EnrollmentRepository enrollmentRepository;

    public CheckInConsumer(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @RabbitListener(queues = CheckInChannel.QUEUE)
    @Transactional
    public void handler(CheckInMessageDTO dto) {
        try {
            Enrollment enrollment = enrollmentRepository.findById(dto.enrollmentId())
                    .orElseThrow(() -> new DomainException("O usuário não está inscrito no evento, mas o check-in foi emitido."));

            enrollment.checkIn();
        } catch (DomainException e) {
            log.error("\nCódigo: {}\nMessage: {}", e.getStatus(), e.getMessage());
        }
    }
}
