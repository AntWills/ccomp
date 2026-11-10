package com.ccomp.br.domain.events.certificate.persistence;

import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityCertificateTemplateRepository extends JpaRepository<ActivityCertificateTemplate, Long> {
    Optional<ActivityCertificateTemplate> findByActivityIdAndTemplateType(Long activityId,
                                                                          EnumActivityCertificateTemplateType templateType);
    List<ActivityCertificateTemplate> findByActivityIdAndTemplateTypeIn(Long activityId,
                                                                        List<EnumActivityCertificateTemplateType> templateTypes);
    List<ActivityCertificateTemplate> findAllByActivityIdIn(List<Long> activityIds);
}
