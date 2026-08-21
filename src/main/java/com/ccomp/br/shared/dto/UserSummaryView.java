package com.ccomp.br.shared.dto;

import com.ccomp.br.module.email.EmailAddress;

import java.util.UUID;

public interface UserSummaryView {
    UUID getId();
    String getName();
    EmailAddress getEmailAddress();
}
