package com.ccomp.br.domain.events.guests.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityGuestRepository extends JpaRepository<ActivityGuest, Long> {
    Optional<ActivityGuest> findByActivityIdAndId(Long activityId, Long id);

    boolean existsByActivityIdAndEventGuestUserId(Long activityId, java.util.UUID userId);

    boolean existsByActivityIdAndEventGuestId(Long activityId, Long eventGuestId);

    Optional<ActivityGuest> findByActivityIdAndEventGuestId(Long activityId, Long eventGuestId);
}
