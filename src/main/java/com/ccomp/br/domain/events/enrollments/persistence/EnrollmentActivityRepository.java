package com.ccomp.br.domain.events.enrollments.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentActivityRepository extends JpaRepository<EnrollmentActivity, Long> {

    @Query("select activity.id from EnrollmentActivity ea join ea.activity activity " +
            "where ea.enrollment.id = :enrollmentId order by activity.id")
    List<Long> findActivityIdsByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
}
