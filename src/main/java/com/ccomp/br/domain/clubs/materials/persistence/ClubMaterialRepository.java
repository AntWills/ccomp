package com.ccomp.br.domain.clubs.materials.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClubMaterialRepository extends JpaRepository<ClubMaterial, Long> {
    List<ClubMaterial> findAllByClubId(Long clubId);
    Optional<ClubMaterial> findByIdAndClubId(Long id, Long clubId);
}
