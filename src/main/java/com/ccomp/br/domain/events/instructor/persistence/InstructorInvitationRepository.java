package com.ccomp.br.domain.events.instructor.persistence;

import com.ccomp.br.module.email.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface InstructorInvitationRepository extends JpaRepository<InstructorInvitation,Long> {
    Optional<InstructorInvitation> findByCode(UUID code);

    Optional<InstructorInvitation> findFirstByEmailAddressAndActivity_IdOrderByInvitedAtDesc(
            EmailAddress email,
            Long activityId
    );

    java.util.List<InstructorInvitation> findAllByActivity_IdOrderByInvitedAtDesc(Long activityId);
}
