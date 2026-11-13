package com.ccomp.br.domain.events.shared.application;

import com.ccomp.br.domain.events.activities.dto.EventActivityConflictCursor;
import com.ccomp.br.domain.events.activities.dto.EventActivityCursor;
import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.utils.EventMapper;
import com.ccomp.br.domain.events.enrollments.dto.UserActivitySummaryDTO;
import com.ccomp.br.domain.events.enrollments.dto.EnrollmentActivityCursor;
import com.ccomp.br.domain.events.enrollments.dto.ActivityEnrollmentSearchResult;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivity;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityDslRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentActivityRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityDslRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.enrollments.persistence.EnrollmentRepository;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ConflictException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.SchedulingConflictException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ActivitiesEnrollmentsServices {
    private final EventActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentActivityRepository enrollmentActivityRepository;
    private final EnrollmentActivityDslRepository enrollmentActivityDslRepository;
    private final EventActivityDslRepository eventActivityDslRepository;
    private final EventAccessPolicy eventAccessPolicy;
    private final EventMapper eventMapper;
    private final Clock clock;

    public ActivitiesEnrollmentsServices(
            EventActivityRepository activityRepository,
            EnrollmentRepository enrollmentRepository,
            EnrollmentActivityRepository enrollmentActivityRepository,
            EnrollmentActivityDslRepository enrollmentActivityDslRepository,
            EventActivityDslRepository eventActivityDslRepository,
            EventAccessPolicy eventAccessPolicy, EventMapper eventMapper, Clock clock) {
        this.activityRepository = activityRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentActivityRepository = enrollmentActivityRepository;
        this.enrollmentActivityDslRepository = enrollmentActivityDslRepository;
        this.eventActivityDslRepository = eventActivityDslRepository;
        this.eventAccessPolicy = eventAccessPolicy;
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    @Transactional
    public MessageResponse inscribe(UUID userId, Long activityId) {
        EventActivity activitySnapshot = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        if (activitySnapshot.isPublic())
            return new MessageResponse("Atividade pública não precisa de inscrição.");

        Event event = activitySnapshot.getEvent();
        var enrollment = enrollmentRepository.findForActivityRegistrationByUserIdAndEventId(userId, event.getId())
                .orElseThrow(() -> new ConflictException("Você precisa estar inscrito no evento."));
        if (!enrollment.isActive())
            throw new ConflictException("Você precisa estar inscrito no evento.");

        EventActivity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        if (activity.isPublic())
            return new MessageResponse("Atividade pública não precisa de inscrição.");

        Optional<EnrollmentActivity> enrollmentOpt = enrollmentActivityDslRepository
                .findByUserIdAndActivityId(userId, activityId);

        if(enrollmentOpt.isPresent())
            throw new ConflictException("Você já está inscrito na atividade.");

        if (event.isSchedulerPrevent()) {
            List<ActivityDTO> conflicts = eventActivityDslRepository.findAllConflictingActivities(userId, activity);
            if (!conflicts.isEmpty())
                throw new SchedulingConflictException(conflicts);
        }

        if (activity.getMaxCapacity() != null
                && activity.getActiveEnrollmentCount() >= activity.getMaxCapacity())
            throw new ConflictException("As vagas para esta atividade estão esgotadas.");

        enrollmentActivityRepository.save(EnrollmentActivity.builder()
                        .activity(activity)
                        .enrollment(enrollment)
                .build()
        );
        activity.registerActiveEnrollment();

        return new MessageResponse("Inscrição na atividade realizada com sucesso.");
    }

    @Transactional
    public MessageResponse enrollParticipant(UUID managerId, Long activityId, Long enrollmentId) {
        EventActivity snapshot = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        Event event = snapshot.getEvent();
        if (!eventAccessPolicy.canEdit(eventMapper.eventToEventDTO(event, LocalDateTime.now(clock)), managerId))
            throw new AccessDeniedException("Você não possui permissão para gerenciar esta atividade.");

        EventActivity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        Enrollment enrollment = enrollmentRepository.findByIdAndEventId(enrollmentId, event.getId())
                .filter(Enrollment::isActive)
                .orElseThrow(() -> new ConflictException("A inscrição ativa não pertence a este evento."));
        if (enrollmentActivityRepository.findByEnrollmentIdAndActivityId(enrollmentId, activityId).isPresent())
            throw new ConflictException("A pessoa já está inscrita na atividade.");

        if (event.isSchedulerPrevent()) {
            List<ActivityDTO> conflicts = eventActivityDslRepository
                    .findAllConflictingActivities(enrollment.getUserId(), activity);
            if (!conflicts.isEmpty()) throw new SchedulingConflictException(conflicts);
        }
        if (activity.getMaxCapacity() != null
                && activity.getActiveEnrollmentCount() >= activity.getMaxCapacity())
            throw new ConflictException("As vagas para esta atividade estão esgotadas.");

        enrollmentActivityRepository.save(EnrollmentActivity.builder()
                .activity(activity).enrollment(enrollment).build());
        activity.registerActiveEnrollment();
        return new MessageResponse("Pessoa inscrita na atividade com sucesso.");
    }

    @Transactional(readOnly = true)
    public CursorPage<ActivityDTO> listSubscriptionsByCursor(
            UUID userId, Long eventId, String cursor, int pageLimit
    ) {
        int finalPageLimit = Math.min(pageLimit, 50);

        EventActivityCursor cursorDecoded = CursorUtils.decode(cursor, EventActivityCursor.class);

        List<ActivityDTO> results = eventActivityDslRepository
                .findSubscriptionsWithCursor(userId, eventId, cursorDecoded, finalPageLimit + 1);

        return CursorUtils.buildPage(
                results,
                finalPageLimit,
                ea -> new EventActivityCursor(ea.displayOrder(), ea.id())
        );
    }

    @Transactional(readOnly = true)
    public CursorPage<ActivityDTO> listConflictingActivities(
            UUID userId, Long activityId, String cursor, int pageLimit
    ) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        int finalPageLimit = Math.min(pageLimit, 50);

        EventActivityConflictCursor cursorDecoded = CursorUtils.decode(cursor, EventActivityConflictCursor.class);
        List<ActivityDTO> results = eventActivityDslRepository
                .findConflictingActivities(userId, activity, cursorDecoded, finalPageLimit + 1);

        return CursorUtils.buildPage(
                results,
                finalPageLimit,
                ea -> new EventActivityConflictCursor(ea.id(), ea.startDate())
        );
    }

    @Transactional
    public MessageResponse unsubscribe(UUID userId, Long activityId) {
        EnrollmentActivity enrollmentActivity = enrollmentActivityDslRepository
                .findByUserIdAndActivityId(userId, activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Você não está incrito neste evento."));

        EventActivity activitySnapshot = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        Event event = activitySnapshot.getEvent();
        enrollmentRepository.findByIdAndEventId(
                        enrollmentActivity.getEnrollmentId(), event.getId())
                .filter(Enrollment::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Você não está incrito nesta atividade."));
        EventActivity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        EnrollmentActivity activityEnrollment = enrollmentActivityDslRepository
                .findByUserIdAndActivityId(userId, activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Você não está incrito nesta atividade."));

        enrollmentActivityRepository.delete(activityEnrollment);
        activity.releaseActiveEnrollment();

        return new MessageResponse("Inscrição removida com sucesso.");
    }

    public CursorPage<UserActivitySummaryDTO> findAllUsersFromActivity(UUID userId, Long activityId, String cursor,
                                                                       int pageSize, String searchTerm) {
        pageSize = Math.min(pageSize, 50);

        Event event = eventActivityDslRepository.findEventByActivityId(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if(!eventAccessPolicy.canEdit(eventMapper.eventToEventDTO(event, LocalDateTime.now(clock)), userId))
            throw new AccessDeniedException("Você não possui permissão para visualizar os incritos.");

        EnrollmentActivityCursor cursorDecoded = CursorUtils.decode(cursor, EnrollmentActivityCursor.class);
        String normalizedTerm = searchTerm == null ? null : searchTerm.trim();
        if (normalizedTerm != null && normalizedTerm.isBlank()) normalizedTerm = null;
        if (normalizedTerm != null && cursorDecoded != null && cursorDecoded.id() != null
                && cursorDecoded.relevance() == null) {
            throw new IllegalArgumentException("O cursor não corresponde a uma busca por texto.");
        }
        List<ActivityEnrollmentSearchResult> result = enrollmentActivityDslRepository
                .findAllUsersByActivityId(activityId, normalizedTerm, cursorDecoded, pageSize + 1);

        return CursorUtils.buildPage(
                result,
                pageSize,
                ActivityEnrollmentSearchResult::enrollment,
                ua -> new EnrollmentActivityCursor(ua.relevance(), ua.enrollment().id(), ua.enrollment().createdAt()));
    }
}
