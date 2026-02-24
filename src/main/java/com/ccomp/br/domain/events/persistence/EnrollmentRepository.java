package com.ccomp.br.domain.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.event WHERE e.userId = :userId")
    List<Enrollment> findAllByUserIdWithEvent(@Param("userId") UUID userId);
}