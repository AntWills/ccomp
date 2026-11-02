package com.ccomp.br.domain.events.activities.materials.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ActivityMaterialRepository extends JpaRepository<ActivityMaterial, Long> {
    List<ActivityMaterial> findAllByActivityIdOrderByCreatedAtDescIdDesc(Long activityId);
    Optional<ActivityMaterial> findByIdAndActivityId(Long id, Long activityId);
}
