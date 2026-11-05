package com.ccomp.br.domain.events.credential.dto;

import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;

public record CredentialSearchItem(EnrollmentListItem enrollment, boolean eligible, boolean credentialed) {}
