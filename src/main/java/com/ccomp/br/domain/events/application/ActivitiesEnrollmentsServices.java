package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.activities.*;
import com.ccomp.br.domain.events.persistence.enrollments.Enrollment;
import com.ccomp.br.domain.events.persistence.enrollments.EnrollmentRepository;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ActivitiesEnrollmentsServices {
    public final EventActivityRepository activityRepository;
    public final EnrollmentRepository enrollmentRepository;
    public final EnrollmentsServices enrollmentsServices;
    public final EnrollmentActivityRepository enrollmentActivityRepository;
    public final EnrollmentActivityDslRepository enrollmentActivityDslRepository;

    public ActivitiesEnrollmentsServices(EventActivityRepository activityRepository, EnrollmentRepository enrollmentRepository, EnrollmentsServices enrollmentsServices, EnrollmentActivityRepository enrollmentActivityRepository, EnrollmentActivityDslRepository enrollmentActivityDslRepository) {
        this.activityRepository = activityRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentsServices = enrollmentsServices;
        this.enrollmentActivityRepository = enrollmentActivityRepository;
        this.enrollmentActivityDslRepository = enrollmentActivityDslRepository;
    }

    @Transactional
    public MessageResponse inscribe(UUID userId, Long activityId) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        if(activity.isPublic())
            return new MessageResponse("Atividade pública não precisa de inscrição.");

        Optional<EnrollmentActivity> enrollmentOpt = enrollmentActivityDslRepository
                .findByUserIdAndActivityId(userId, activityId);

        if(enrollmentOpt.isPresent())
            throw new ConflictException("Você já está inscrito na atividade.");

        Event event = activity.getEvent();

        var enrollment = enrollmentRepository.findByUserIdAndEvent(userId, event)
                .orElseThrow(() -> new ConflictException("Você precisa estar inscrito no evento."));

        enrollmentActivityRepository.save(EnrollmentActivity.builder()
                        .activity(activity)
                        .enrollment(enrollment)
                .build()
        );

        return new MessageResponse("Inscrição na atividade realizada com sucesso.");
    }
}
