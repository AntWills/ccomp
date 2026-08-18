package com.ccomp.br.domain.clubs.persistence.members;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long>, JpaSpecificationExecutor<ClubMember> {
    List<ClubMember> findByUserId(UUID userId);
    List<ClubMember> findByUserIdAndClubId(UUID userId, Long clubId);
}