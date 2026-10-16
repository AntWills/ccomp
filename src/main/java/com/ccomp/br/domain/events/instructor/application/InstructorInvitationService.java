package com.ccomp.br.domain.events.instructor.application;

import com.ccomp.br.config.rabbit.RabbitMQConfig;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.instructor.external.InstructorInvitationMessageDTO;
import com.ccomp.br.domain.events.instructor.external.message.InstructorInvitationChannel;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.events.instructor.persistence.InstructorInvitation;
import com.ccomp.br.domain.events.instructor.persistence.InstructorInvitationRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.exceptions.UserBlockedException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InstructorInvitationService {
    private final EventActivityRepository activityRepository;
    private final EventInstructorRepository instructorRepository;
    private final InstructorInvitationRepository invitationRepository;
    private final EventCache eventCache;
    private final EventAccessPolicy eventAccessPolicy;
    private final UserManagement userManagement;
    private final RabbitTemplate rabbitTemplate;

    public InstructorInvitationService(
            EventActivityRepository activityRepository,
            EventInstructorRepository instructorRepository,
            InstructorInvitationRepository invitationRepository,
            EventCache eventCache,
            EventAccessPolicy eventAccessPolicy,
            UserManagement userManagement,
            RabbitTemplate rabbitTemplate
    ) {
        this.activityRepository = activityRepository;
        this.instructorRepository = instructorRepository;
        this.invitationRepository = invitationRepository;
        this.eventCache = eventCache;
        this.eventAccessPolicy = eventAccessPolicy;
        this.userManagement = userManagement;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public MessageResponse invite(UUID requesterId, Long activityId, EmailAddress emailAddress) {
        EventActivity activity = findActivity(activityId);
        EventDTO event = getEvent(activity);
        assertCanManage(event, requesterId);

        Optional<UserDTO> invitedUser = userManagement.findByEmailAddress(emailAddress);
        if (invitedUser.isPresent() && !invitedUser.get().isActive()) {
            throw new UserBlockedException("O usuário informado está inativo ou bloqueado.");
        }

        if (invitedUser.isPresent()
                && instructorRepository.findByActivityIdAndUserId(activityId, invitedUser.get().id())
                .filter(EventInstructor::isConfirmed)
                .isPresent()) {
            return new MessageResponse("Este usuário já ministra esta atividade.");
        }

        invitationRepository
                .findFirstByEmailAddressAndActivity_IdOrderByInvitedAtDesc(emailAddress, activityId)
                .filter(InstructorInvitation::isSent)
                .ifPresent(invitation -> {
                    invitation.cancel();
                    invitationRepository.save(invitation);
                });

        UUID code = UUID.randomUUID();
        InstructorInvitation invitation = InstructorInvitation.builder()
                .code(code)
                .activity(activity)
                .emailAddress(emailAddress)
                .invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        invitationRepository.save(invitation);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                InstructorInvitationChannel.ROUTING,
                new InstructorInvitationMessageDTO(
                        event.id(),
                        event.title(),
                        activity.getId(),
                        activity.getTitle(),
                        code,
                        emailAddress
                )
        );

        return new MessageResponse("Um e-mail de convite foi enviado.");
    }

    @Transactional
    public MessageResponse reply(UUID code, UUID userId, boolean accept) {
        InstructorInvitation invitation = invitationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Código de convite não encontrado."));
        if (!invitation.isValid()) {
            throw new DomainException("Convite inválido ou expirado.");
        }

        UserDTO user = userManagement.findByEmailAddress(invitation.getEmailAddress())
                .orElseThrow(() -> new UserNotFoundException("Usuário deve estar cadastrado no sistema."));
        if (!user.isActive()) {
            throw new UserBlockedException("O usuário informado está inativo ou bloqueado.");
        }
        if (!user.id().equals(userId) || !invitation.isSameEmail(user.emailAddress())) {
            throw new AccessDeniedException("Este convite pertence a outra conta.");
        }

        if (!accept) {
            invitation.reject();
            invitationRepository.save(invitation);
            return new MessageResponse("Convite recusado.");
        }

        EventInstructor instructor = instructorRepository
                .findByActivityIdAndUserId(invitation.getActivity().getId(), userId)
                .orElseGet(() -> EventInstructor.builder()
                        .activityId(invitation.getActivity().getId())
                        .userId(userId)
                        .build());

        if (instructor.isCanceled()) {
            instructor.reactivate();
        }

        instructorRepository.save(instructor);
        invitation.accept();
        invitationRepository.save(invitation);
        return new MessageResponse("Convite aceito. Você agora ministra esta atividade.");
    }

    @Transactional(readOnly = true)
    public List<InstructorInvitation> searchInvitations(UUID requesterId, Long activityId) {
        EventActivity activity = findActivity(activityId);
        assertCanManage(getEvent(activity), requesterId);
        return invitationRepository.findAllByActivity_IdOrderByInvitedAtDesc(activityId);
    }

    @Transactional
    public MessageResponse cancel(UUID requesterId, Long invitationId) {
        InstructorInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado."));
        assertCanManage(getEvent(invitation.getActivity()), requesterId);
        invitation.cancel();
        invitationRepository.save(invitation);
        return new MessageResponse("Convite cancelado com sucesso.");
    }

    private EventActivity findActivity(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
    }

    private EventDTO getEvent(EventActivity activity) {
        return eventCache.findById(activity.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
    }

    private void assertCanManage(EventDTO event, UUID requesterId) {
        if (!eventAccessPolicy.canEdit(event, requesterId)) {
            throw new AccessDeniedException(
                    "Você não tem permissão para gerenciar ministrantes deste evento."
            );
        }
    }
}
