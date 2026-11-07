package com.ccomp.br.domain.events.instructor.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.instructor.dto.ActivityParticipantDTO;
import com.ccomp.br.domain.events.instructor.dto.InstructorCursor;
import com.ccomp.br.domain.events.instructor.dto.EventInstructorDTO;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorStatus;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.events.instructor.persistence.InstructorDslRepository;
import com.ccomp.br.domain.events.guests.persistence.dsl.ActivityGuestDslRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstructorService {
    private final EventActivityRepository activityRepository;
    private final EventInstructorRepository instructorRepository;
    private final InstructorDslRepository instructorDslRepository;
    private final ActivityGuestDslRepository activityGuestDslRepository;
    private final EventCache eventCache;
    private final EventAccessPolicy eventAccessPolicy;
    private final UserManagement userManagement;

    public InstructorService(
            EventActivityRepository activityRepository,
            EventInstructorRepository instructorRepository,
            InstructorDslRepository instructorDslRepository,
            ActivityGuestDslRepository activityGuestDslRepository,
            EventCache eventCache,
            EventAccessPolicy eventAccessPolicy,
            UserManagement userManagement
    ) {
        this.activityRepository = activityRepository;
        this.instructorRepository = instructorRepository;
        this.instructorDslRepository = instructorDslRepository;
        this.activityGuestDslRepository = activityGuestDslRepository;
        this.eventCache = eventCache;
        this.eventAccessPolicy = eventAccessPolicy;
        this.userManagement = userManagement;
    }

    @Transactional(readOnly = true)
    public CursorPage<EventInstructorDTO> searchByEvent(
            Long eventId,
            UUID requesterId,
            String cursor,
            int pageSize
    ) {
        EventDTO event = eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));

        if (!eventAccessPolicy.canView(event, requesterId)) {
            throw new AccessDeniedException(
                    "Você não tem permissão para visualizar os ministrantes deste evento."
            );
        }

        int limit = Math.min(pageSize, 50);
        InstructorCursor decodedCursor = CursorUtils.decode(cursor, InstructorCursor.class);
        List<EventInstructorDTO> instructors = instructorDslRepository
                .findByEvent(eventId, decodedCursor, limit + 1);

        CursorPage<EventInstructorDTO> page = CursorUtils.buildPage(
                instructors,
                limit,
                instructor -> new InstructorCursor(instructor.id(), instructor.createdAt())
        );
        return page;
    }

    @Transactional(readOnly = true)
    public List<ActivityParticipantDTO> searchByActivity(Long activityId, UUID requesterId) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        EventDTO event = eventCache.findById(activity.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (!eventAccessPolicy.canView(event, requesterId))
            throw new AccessDeniedException("Você não tem permissão para visualizar os ministrantes desta atividade.");

        boolean manager = eventAccessPolicy.canEdit(event, requesterId);
        ArrayList<ActivityParticipantDTO> result = new ArrayList<>();
        result.addAll(instructorDslRepository.findByActivity(activityId));
        result.addAll(activityGuestDslRepository.findActivityGuestsForParticipants(activityId, manager));
        result.sort(Comparator.comparing(ActivityParticipantDTO::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Transactional(readOnly = true)
    public boolean canEditActivity(Long activityId, UUID requesterId) {
        return requesterId != null
                && userManagement.isAccountActive(requesterId)
                && instructorRepository.findByActivityIdAndUserId(activityId, requesterId)
                .filter(EventInstructor::isConfirmed)
                .isPresent();
    }

    @Transactional
    public MessageResponse cancelMyLink(UUID instructorId, Long activityId) {
        EventInstructor instructor = instructorRepository.findByActivityIdAndUserId(activityId, instructorId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Você não está vinculado como ministrante desta atividade."
                ));

        if (!instructor.isConfirmed()) {
            throw new DomainException("Seu vínculo de ministrante já está cancelado.");
        }

        instructor.cancel();
        instructorRepository.save(instructor);
        return new MessageResponse("Seu vínculo como ministrante foi cancelado.");
    }

    @Transactional
    public void deleteInstructor(Long activityId, Long instructorId, UUID requesterId) {
        EventInstructor instructor = instructorRepository.findByIdAndActivityId(instructorId, activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Ministrante não encontrado nesta atividade."));
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        EventDTO event = eventCache.findById(activity.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        if (!eventAccessPolicy.canEdit(event, requesterId)) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar ministrantes deste evento.");
        }
        instructorRepository.delete(instructor);
    }
}
