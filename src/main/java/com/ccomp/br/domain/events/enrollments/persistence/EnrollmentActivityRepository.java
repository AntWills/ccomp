package com.ccomp.br.domain.events.enrollments.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentActivityRepository extends JpaRepository<EnrollmentActivity, Long> {
    Optional<EnrollmentActivity> findByEnrollmentIdAndActivityId(Long enrollmentId, Long activityId);


    @Query("select activity.id from EnrollmentActivity ea join ea.activity activity " +
            "where ea.enrollment.id = :enrollmentId and ea.canceled = false order by activity.id")
    List<Long> findNonCanceledActivityIdsByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
}
