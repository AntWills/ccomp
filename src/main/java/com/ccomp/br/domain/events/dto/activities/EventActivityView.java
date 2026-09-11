package com.ccomp.br.domain.events.dto.activities;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import com.ccomp.br.domain.events.enums.activities.EnumActivityRegistrationPolicy;
import com.ccomp.br.domain.events.enums.activities.EnumActivityType;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;

import java.time.LocalDateTime;

@EntityView(EventActivity.class)
public interface EventActivityView {
    @IdMapping
    Long getId();

    @Mapping("event.id")
    Long getEventId();

    String getTitle();

    String getDescription();

    Long getDisplayOrder();

    String getLocation();

    LocalDateTime getStartDate();

    LocalDateTime getEndDate();

    EnumActivityRegistrationPolicy getRegistrationPolicy();

    EnumActivityType getType();

    LocalDateTime getCreatedAt();
}
