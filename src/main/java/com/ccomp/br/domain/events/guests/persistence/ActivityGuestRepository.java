package com.ccomp.br.domain.events.guests.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityGuestRepository extends JpaRepository<ActivityGuest, Long> {
    boolean existsByActivityIdAndEventGuestUserId(Long activityId, java.util.UUID userId);

    boolean existsByActivityIdAndEventGuestId(Long activityId, Long eventGuestId);
}
