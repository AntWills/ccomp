package com.ccomp.br.shared.dto;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.ccomp.br.domain.events.core.enums.EnumEventCategory;
import com.ccomp.br.domain.events.core.enums.EnumEventFormat;
import com.ccomp.br.domain.events.core.enums.EnumEventStatus;
import com.ccomp.br.domain.events.core.persistence.Event;

import java.time.LocalDateTime;

@EntityView(Event.class)
public interface EventListItemView {

    @IdMapping
    Long getId();

    String getTitle();

    String getSlug();

    String getSummary();

    String getCoverImageUrl();

    EnumEventFormat getFormat();

    EnumEventCategory getCategory();

    EnumEventStatus getStatus();

    LocalDateTime getStartDate();

    LocalDateTime getEndDate();

    LocalDateTime getEnrollmentStartDate();

    LocalDateTime getEnrollmentEndDate();
}