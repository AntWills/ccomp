package com.ccomp.br.domain.events.activities.persistence.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    Optional<CheckIn> findByActivityId(long activityId);
}