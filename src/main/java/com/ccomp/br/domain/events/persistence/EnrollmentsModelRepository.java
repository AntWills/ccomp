package com.ccomp.br.domain.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EnrollmentsModelRepository extends JpaRepository<EnrollmentsModel, Long> {
    @Query("SELECT e FROM EnrollmentModel e JOIN FETCH e.event WHERE e.userId = :userId")
    List<EnrollmentsModel> findAllByUserIdWithEvent(@Param("userId") UUID userId);
}