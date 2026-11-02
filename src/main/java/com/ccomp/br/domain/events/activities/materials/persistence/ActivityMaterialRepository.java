package com.ccomp.br.domain.events.activities.materials.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ActivityMaterialRepository extends JpaRepository<ActivityMaterial, Long> {
    Optional<ActivityMaterial> findByIdAndActivityId(Long id, Long activityId);
}
