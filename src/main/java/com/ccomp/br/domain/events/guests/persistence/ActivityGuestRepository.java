package com.ccomp.br.domain.events.guests.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityGuestRepository extends JpaRepository<ActivityGuest, Long> {
}