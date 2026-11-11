package com.ccomp.br.domain.events.certificate.application;

import com.ccomp.br.domain.events.certificate.dto.BoxElementDTO;
import com.ccomp.br.domain.events.certificate.enums.EnumActivityCertificateTemplateType;
import com.ccomp.br.domain.storage.external.StorageExternal;

import java.util.List;

public record ResolvedActivityCertificateTemplate(
        EnumActivityCertificateTemplateType context,
        List<String> variables,
        List<BoxElementDTO> boxes,
        StorageExternal.StoredFile cover,
        boolean activitySpecific
) {}
