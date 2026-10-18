package com.ccomp.br.domain.events.instructor.web;

import com.ccomp.br.domain.events.activities.dto.ActivityDTO;
import com.ccomp.br.domain.events.activities.dto.UpdateActivityDTO;
import com.ccomp.br.domain.events.instructor.application.InstructorInvitationService;
import com.ccomp.br.domain.events.instructor.application.InstructorService;
import com.ccomp.br.domain.events.instructor.persistence.EventInstructor;
import com.ccomp.br.domain.events.instructor.persistence.InstructorInvitation;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.utils.CursorPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class InstructorController {
    private final InstructorService instructorService;
    private final InstructorInvitationService invitationService;

    public InstructorController(
            InstructorService instructorService,
            InstructorInvitationService invitationService
    ) {
        this.instructorService = instructorService;
        this.invitationService = invitationService;
    }

    @GetMapping("/{eventId}/instructors")
    public ResponseEntity<CursorPage<EventInstructor>> searchByEvent(
            @RequestAttribute(value = "userId", required = false) UUID userId,
            @PathVariable Long eventId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(
                instructorService.searchByEvent(eventId, userId, cursor, pageSize)
        );
    }

    @PatchMapping("/activities/{activityId}/instructors/me")
    public ResponseEntity<ActivityDTO> updateActivityAsInstructor(
            @RequestAttribute("userId") UUID userId,
            @PathVariable Long activityId,
            @Valid @RequestBody UpdateActivityDTO request
    ) {
        return ResponseEntity.ok(
                instructorService.updateActivity(userId, activityId, request)
        );
    }

    @DeleteMapping("/activities/{activityId}/instructors/me")
    public ResponseEntity<MessageResponse> cancelMyInstructorLink(
            @RequestAttribute("userId") UUID userId,
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(instructorService.cancelMyLink(userId, activityId));
    }

    @PostMapping("/activities/{activityId}/instructors/invitations")
    public ResponseEntity<MessageResponse> invite(
            @RequestAttribute("userId") UUID userId,
            @PathVariable Long activityId,
            @Valid @RequestBody InviteInstructorRequest request
    ) {
        return ResponseEntity.ok(invitationService.invite(
                userId,
                activityId,
                new EmailAddress(request.email())
        ));
    }

    @GetMapping("/activities/{activityId}/instructors/invitations")
    public ResponseEntity<List<InstructorInvitation>> searchInvitations(
            @RequestAttribute("userId") UUID userId,
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(invitationService.searchInvitations(userId, activityId));
    }

    @PostMapping("/instructor-invitations/{code}/reply")
    public ResponseEntity<MessageResponse> reply(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID code,
            @RequestParam(defaultValue = "true") boolean accept
    ) {
        return ResponseEntity.ok(invitationService.reply(code, userId, accept));
    }

    @DeleteMapping("/instructor-invitations/{invitationId}")
    public ResponseEntity<MessageResponse> cancel(
            @RequestAttribute("userId") UUID userId,
            @PathVariable Long invitationId
    ) {
        return ResponseEntity.ok(invitationService.cancel(userId, invitationId));
    }

    public record InviteInstructorRequest(
            @NotBlank @Email String email
    ) {
    }
}
