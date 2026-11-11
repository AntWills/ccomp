package com.ccomp.br.domain.events.instructor.application;

import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.core.persistence.EventRepository;
import com.ccomp.br.domain.events.instructor.dto.InstructorActivityLinkDTO;
import com.ccomp.br.domain.events.instructor.dto.InstructorProfileDTO;
import com.ccomp.br.domain.events.instructor.dto.UpdateInstructorProfileRequest;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorProfile;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorProfileRepository;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserBlockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InstructorProfileService {
    private final EventCache eventCache;
    private final EventRepository eventRepository;
    private final EventAccessPolicy eventAccessPolicy;
    private final UserManagement userManagement;
    private final EventInstructorRepository instructorRepository;
    private final EventInstructorProfileRepository profileRepository;

    public InstructorProfileService(EventCache eventCache, EventRepository eventRepository,
                                    EventAccessPolicy eventAccessPolicy, UserManagement userManagement,
                                    EventInstructorRepository instructorRepository,
                                    EventInstructorProfileRepository profileRepository) {
        this.eventCache = eventCache;
        this.eventRepository = eventRepository;
        this.eventAccessPolicy = eventAccessPolicy;
        this.userManagement = userManagement;
        this.instructorRepository = instructorRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<InstructorProfileDTO> listProfiles(Long eventId, UUID requesterId) {
        EventDTO event = findEvent(eventId);
        if (!eventAccessPolicy.canView(event, requesterId))
            throw new AccessDeniedException("Você não tem permissão para visualizar os ministrantes deste evento.");
        return profileRepository.findAllByEventIdOrderByCreatedAtDescIdDesc(eventId).stream()
                .map(this::toProfileDTO).toList();
    }

    @Transactional
    public InstructorProfileDTO createProfile(Long eventId, UUID requesterId, String name,
                                              String description, String accountEmail) {
        assertCanEdit(findEvent(eventId), requesterId);
        UUID userId = resolveAccountId(accountEmail);
        if (userId != null && profileRepository.findByEventIdAndUserId(eventId, userId).isPresent())
            throw new DomainException("Esta conta já possui um perfil de ministrante neste evento.");
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
        EventInstructorProfile profile = profileRepository.save(EventInstructorProfile.builder()
                .event(event).userId(userId).name(name).description(description).build());
        return toProfileDTO(profile);
    }

    @Transactional
    public InstructorProfileDTO updateProfile(Long eventId, Long profileId, UUID requesterId,
                                              UpdateInstructorProfileRequest request) {
        assertCanEdit(findEvent(eventId), requesterId);
        EventInstructorProfile profile = findProfile(eventId, profileId);
        if (request.name() != null) profile.setName(request.name());
        if (request.description() != null) profile.setDescription(request.description());
        profileRepository.save(profile);
        for (EventInstructor link : instructorRepository.findAllByProfile_Id(profileId)) {
            link.setName(profile.getName());
            link.setDescription(profile.getDescription());
            instructorRepository.save(link);
        }
        return toProfileDTO(profile);
    }

    @Transactional
    public InstructorProfileDTO linkProfileAccount(Long eventId, Long profileId, UUID requesterId, String email) {
        assertCanEdit(findEvent(eventId), requesterId);
        EventInstructorProfile profile = findProfile(eventId, profileId);
        UUID accountId = resolveAccountId(email);
        profileRepository.findByEventIdAndUserId(eventId, accountId)
                .filter(other -> !other.getId().equals(profileId))
                .ifPresent(other -> { throw new DomainException("Esta conta já possui outro perfil de ministrante neste evento."); });
        profile.setUserId(accountId);
        profileRepository.save(profile);
        for (EventInstructor link : instructorRepository.findAllByProfile_Id(profileId)) {
            link.setUserId(accountId);
            link.setName(profile.getName());
            link.setDescription(profile.getDescription());
            instructorRepository.save(link);
        }
        return toProfileDTO(profile);
    }

    @Transactional
    public void deleteProfile(Long eventId, Long profileId, UUID requesterId) {
        assertCanEdit(findEvent(eventId), requesterId);
        EventInstructorProfile profile = findProfile(eventId, profileId);
        instructorRepository.deleteAll(instructorRepository.findAllByProfile_Id(profileId));
        profileRepository.delete(profile);
    }

    private UUID resolveAccountId(String email) {
        if (email == null || email.isBlank()) return null;
        UserDTO account = userManagement.findByEmailAddress(new EmailAddress(email))
                .orElseThrow(() -> new ResourceNotFoundException("Não há uma conta ativa com este e-mail."));
        if (!account.isActive()) throw new UserBlockedException("A conta informada está inativa ou bloqueada.");
        return account.id();
    }

    private EventDTO findEvent(Long eventId) {
        return eventCache.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
    }

    private EventInstructorProfile findProfile(Long eventId, Long profileId) {
        return profileRepository.findByIdAndEventId(profileId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de ministrante não encontrado."));
    }

    private void assertCanEdit(EventDTO event, UUID requesterId) {
        if (!eventAccessPolicy.canEdit(event, requesterId))
            throw new AccessDeniedException("Você não tem permissão para gerenciar ministrantes deste evento.");
    }

    private InstructorProfileDTO toProfileDTO(EventInstructorProfile profile) {
        List<InstructorActivityLinkDTO> links = instructorRepository.findAllByProfile_Id(profile.getId()).stream()
                .map(link -> new InstructorActivityLinkDTO(link.getId(), link.getActivityId(),
                        link.getRole(), link.getStatus())).toList();
        return new InstructorProfileDTO(profile.getId(), profile.getEvent().getId(), profile.getUserId(),
                profile.getName(), profile.getDescription(), profile.getCreatedAt(), links);
    }
}
