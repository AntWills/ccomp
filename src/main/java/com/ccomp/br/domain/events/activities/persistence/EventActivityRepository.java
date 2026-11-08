package com.ccomp.br.domain.events.activities.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface EventActivityRepository extends JpaRepository<EventActivity, Long> {
    List<EventActivity> findAllByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select activity from EventActivity activity where activity.id = :activityId")
    Optional<EventActivity> findByIdForUpdate(@Param("activityId") Long activityId);
}
