package com.ccomp.br.domain.events.instructor.application;

import com.ccomp.br.config.rabbit.RabbitMQConfig;
import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.persistence.ActivityCache;
import com.ccomp.br.domain.events.activities.persistence.EventActivity;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.core.application.EventAccessPolicy;
import com.ccomp.br.domain.events.core.dto.EventDTO;
import com.ccomp.br.domain.events.core.persistence.EventCache;
import com.ccomp.br.domain.events.instructor.external.InstructorInvitationMessageDTO;
import com.ccomp.br.domain.events.instructor.external.message.InstructorInvitationChannel;
import com.ccomp.br.domain.events.instructor.dto.InstructorInvitationDTO;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructorRepository;
import com.ccomp.br.domain.events.instructor.persistence.InstructorInvitation;
import com.ccomp.br.domain.events.instructor.persistence.InstructorInvitationRepository;
import com.ccomp.br.domain.events.instructor.enums.EnumInstructorRole;
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
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InstructorInvitationService {
    private final ActivityCache activityCache;
    private final EventActivityRepository activityRepository;
    private final EventInstructorRepository instructorRepository;
    private final InstructorInvitationRepository invitationRepository;
    private final EventCache eventCache;
    private final EventAccessPolicy eventAccessPolicy;
    private final UserManagement userManagement;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    public InstructorInvitationService(
            ActivityCache activityCache,
            EventActivityRepository activityRepository,
            EventInstructorRepository instructorRepository,
            InstructorInvitationRepository invitationRepository,
            EventCache eventCache,
            EventAccessPolicy eventAccessPolicy,
            UserManagement userManagement,
            RabbitTemplate rabbitTemplate,
            Clock clock
    ) {
        this.activityCache = activityCache;
        this.activityRepository = activityRepository;
        this.instructorRepository = instructorRepository;
        this.invitationRepository = invitationRepository;
        this.eventCache = eventCache;
        this.eventAccessPolicy = eventAccessPolicy;
        this.userManagement = userManagement;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
    }

    @Transactional
    public MessageResponse invite(UUID requesterId, Long activityId, EmailAddress emailAddress) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
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
                .invitedAt(LocalDateTime.now(clock))
                .expiresAt(LocalDateTime.now(clock).plusHours(24))
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
    public MessageResponse addInstructor(UUID requesterId, Long activityId, String name,
                                         String description, EnumInstructorRole role, String accountEmail) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        assertCanManage(getEvent(activity), requesterId);

        UUID accountId = null;
        if (accountEmail != null && !accountEmail.isBlank()) {
            UserDTO account = userManagement.findByEmailAddress(new EmailAddress(accountEmail))
                    .filter(UserDTO::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Não há uma conta ativa com este e-mail."));
            accountId = account.id();
            Optional<EventInstructor> existing = instructorRepository.findByActivityIdAndUserId(activityId, accountId);
            if (existing.filter(EventInstructor::isConfirmed).isPresent()) {
                return new MessageResponse("Este usuário já ministra esta atividade.");
            }
            if (existing.isPresent()) {
                EventInstructor instructor = existing.get();
                if (instructor.isCanceled()) instructor.reactivate();
                instructor.setName(name);
                instructor.setDescription(description);
                instructor.setRole(role == null ? EnumInstructorRole.INSTRUCTOR : role);
                instructorRepository.save(instructor);
                return new MessageResponse("Vínculo do ministrante reativado.");
            }
        }

        instructorRepository.save(EventInstructor.builder()
                .activityId(activityId)
                .userId(accountId)
                .name(name)
                .description(description)
                .role(role == null ? EnumInstructorRole.INSTRUCTOR : role)
                .build());
        return new MessageResponse("Ministrante adicionado à atividade.");
    }

    @Transactional
    public MessageResponse linkInstructorAccount(UUID requesterId, Long activityId, Long instructorId, String email) {
        EventActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        assertCanManage(getEvent(activity), requesterId);
        EventInstructor instructor = instructorRepository.findById(instructorId)
                .filter(item -> item.getActivityId().equals(activityId))
                .orElseThrow(() -> new ResourceNotFoundException("Ministrante não encontrado nesta atividade."));
        UserDTO account = userManagement.findByEmailAddress(new EmailAddress(email))
                .filter(UserDTO::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Não há uma conta ativa com este e-mail."));
        instructorRepository.findByActivityIdAndUserId(activityId, account.id())
                .filter(other -> !other.getId().equals(instructorId) && other.isConfirmed())
                .ifPresent(other -> { throw new DomainException("Esta conta já está vinculada como ministrante da atividade."); });
        instructor.setUserId(account.id());
        instructorRepository.save(instructor);
        return new MessageResponse("Conta vinculada ao ministrante.");
    }

    @Transactional
    public MessageResponse reply(UUID code, UUID userId, boolean accept) {
        InstructorInvitation invitation = invitationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Código de convite não encontrado."));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!invitation.isValid(now)) {
            throw new DomainException("Convite inválido ou expirado.");
        }

        UserDTO user = userManagement.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário deve estar cadastrado no sistema."));

        if (!user.isActive())
            throw new UserBlockedException("O usuário informado está inativo ou bloqueado.");
        if (!invitation.isSameEmail(user.emailAddress()))
            throw new AccessDeniedException("Este convite pertence a outra conta.");


        if (!accept) {
            invitation.reject(now);
            invitationRepository.save(invitation);
            return new MessageResponse("Convite recusado.");
        }

        EventInstructor instructor = instructorRepository
                .findByActivityIdAndUserId(invitation.getActivity().getId(), userId)
                .orElseGet(() -> EventInstructor.builder()
                        .activityId(invitation.getActivity().getId())
                        .userId(userId)
                        .name(user.name())
                        .build());

        if (instructor.getName() == null || instructor.getName().isBlank()) {
            // Registros antigos vinculados por convite podem não ter nome preenchido.
            // O nome da conta passa a ser o nome público do ministrante.
            instructor.setName(user.name());
        }

        if (instructor.isCanceled()) {
            instructor.reactivate();
        }

        instructorRepository.save(instructor);
        invitation.accept(now);
        invitationRepository.save(invitation);
        return new MessageResponse("Convite aceito. Você agora ministra esta atividade.");
    }

    @Transactional(readOnly = true)
    public List<InstructorInvitationDTO> searchInvitations(UUID requesterId, Long activityId) {
        ActivityDTO activity = activityCache.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada."));
        assertCanManage(getEvent(activity), requesterId);
        return invitationRepository.findAllByActivity_IdOrderByInvitedAtDesc(activityId)
                .stream()
                .map(invitation -> new InstructorInvitationDTO(
                        invitation.getId(),
                        invitation.getActivity().getId(),
                        invitation.getEmailAddress().getValue(),
                        invitation.getStatus(),
                        invitation.getInvitedAt(),
                        invitation.getExpiresAt(),
                        invitation.getRespondedAt()
                ))
                .toList();
    }

    @Transactional
    public MessageResponse cancel(UUID requesterId, Long invitationId) {
        InstructorInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado."));
        assertCanManage(getEvent(invitation.getActivity()), requesterId);

        if (!invitation.isSent())
            throw new DomainException("Não é possível cancelar um convite que já foi respondido ou cancelado.");

        invitation.cancel();
        invitationRepository.save(invitation);
        return new MessageResponse("Convite cancelado com sucesso.");
    }

    private EventDTO getEvent(ActivityDTO activity) {
        return eventCache.findById(activity.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado."));
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
