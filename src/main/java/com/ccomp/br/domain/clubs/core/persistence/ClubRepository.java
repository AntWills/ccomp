package com.ccomp.br.domain.clubs.core.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long>, JpaSpecificationExecutor<Club> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Club c where c.id = :clubId")
    Optional<Club> findByIdForUpdate(@Param("clubId") Long clubId);
}
