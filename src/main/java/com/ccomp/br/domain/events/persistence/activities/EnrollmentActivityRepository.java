package com.ccomp.br.domain.events.persistence.activities;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnrollmentActivityRepository extends JpaRepository<EnrollmentActivity, Long> {

}