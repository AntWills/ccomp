package com.ccomp.br.domain.events.instructor.application;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.dto.UpdateActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.activities.utils.ActivityMapper;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.instructor.dto.InstructorCursor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.events.instructor.persistence.InstructorDslRepository;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InstructorService {
    private final EventActivityRepository activityRepository;
    private final EventInstructorRepository instructorRepository;
    private final InstructorDslRepository instructorDslRepository;
    private final EventCache eventCache;
    private final EventAccessPolicy eventAccessPolicy;
    private final ActivityMapper activityMapper;

    public InstructorService(
            EventActivityRepository activityRepository,
            EventInstructorRepository instructorRepository,
            InstructorDslRepository instructorDslRepository,
            EventCache eventCache,
            EventAccessPolicy eventAccessPolicy,
            ActivityMapper activityMapper
    ) {
        this.activityRepository = activityRepository;
        this.instructorRepository = instructorRepository;
        this.instructorDslRepository = instructorDslRepository;
        this.eventCache = eventCache;
        this.eventAccessPolicy = eventAccessPolicy;
        this.activityMapper = activityMapper;
    }

    @Transactional(readOnly = true)
    public CursorPage<EventInstructor> searchByEvent(
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
        List<EventInstructor> instructors = instructorDslRepository
                .findByEvent(eventId, decodedCursor, limit + 1);

        return CursorUtils.buildPage(
                instructors,
                limit,
                instructor -> new InstructorCursor(instructor.getId(), instructor.getCreatedAt())
        );
    }

    @Transactional
    public ActivityDTO updateActivity(
            UUID instructorId,
            Long activityId,
            UpdateActivityDTO request
    ) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));

        instructorRepository.findByActivityIdAndUserId(activityId, instructorId)
                .filter(EventInstructor::isConfirmed)
                .orElseThrow(() -> new AccessDeniedException(
                        "Você não está vinculado como ministrante desta atividade."
                ));

        activityMapper.updateEventActivityFromRequest(request, activity);
        EventActivity savedActivity = activityRepository.save(activity);

        return activityMapper.eventActivityToActivityDTO(savedActivity);
    }
}
