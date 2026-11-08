package com.ccomp.br.domain.events.guests.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActivityGuestRepository extends JpaRepository<ActivityGuest, Long> {
    boolean existsByActivityIdAndEventGuestUserId(Long activityId, UUID userId);

    boolean existsByActivityIdAndEventGuestId(Long activityId, Long eventGuestId);

    Optional<ActivityGuest> findByActivityIdAndEventGuestId(Long activityId, Long eventGuestId);
}
