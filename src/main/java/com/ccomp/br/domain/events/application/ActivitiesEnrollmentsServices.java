package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.dto.enrollments.EnrollmentsCursor;
import com.ccomp.br.domain.events.dto.enrollments.UserActivitySummaryDTO;
import com.ccomp.br.domain.events.enums.activities.EnrollmentActivityCursor;
import com.ccomp.br.domain.events.persistence.Event;
import com.ccomp.br.domain.events.persistence.activities.*;
import com.ccomp.br.domain.events.persistence.enrollments.EnrollmentRepository;
import com.ccomp.br.domain.security.SecurityUtils;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActivitiesEnrollmentsServices {
    private final EventActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentActivityRepository enrollmentActivityRepository;
    private final EnrollmentActivityDslRepository enrollmentActivityDslRepository;
    private final EventActivityDslRepository eventActivityDslRepository;
    private final EditorServices editorServices;

    public ActivitiesEnrollmentsServices(EventActivityRepository activityRepository, EnrollmentRepository enrollmentRepository, EnrollmentActivityRepository enrollmentActivityRepository, EnrollmentActivityDslRepository enrollmentActivityDslRepository, EventActivityDslRepository eventActivityDslRepository, EditorServices editorServices) {
        this.activityRepository = activityRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentActivityRepository = enrollmentActivityRepository;
        this.enrollmentActivityDslRepository = enrollmentActivityDslRepository;
        this.eventActivityDslRepository = eventActivityDslRepository;
        this.editorServices = editorServices;
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

    public MessageResponse unsubscribe(UUID userId, Long activityId) {
        EnrollmentActivity enrollment = enrollmentActivityDslRepository
                .findByUserIdAndActivityId(userId, activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Você não está incrito neste evento."));

        enrollmentActivityRepository.delete(enrollment);

        return new MessageResponse("Inscrição removida com sucesso.");
    }

    public CursorPage<UserActivitySummaryDTO> findAllUsersFromActivity(UUID userId, Long activityId, String cursor, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        Event event = eventActivityDslRepository.findEventByActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        boolean canEdit = SecurityUtils.isAdmin()
                || event.isOwner(userId)
                || editorServices.hasPermissionEdit(event, userId);

        EnrollmentActivityCursor cursorDecoded = CursorUtils.decode(cursor, EnrollmentActivityCursor.class);
        List<UserActivitySummaryDTO> result = enrollmentActivityDslRepository
                .findAllUsersByActivityId(activityId, cursorDecoded, pageSize + 1);

        return CursorUtils.buildPage(
                result,
                pageSize,
                ua -> new EnrollmentsCursor(ua.createdAt(), ua.id()));
    }
}
