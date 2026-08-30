package com.ccomp.br.domain.events.dto;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;

import java.time.LocalDateTime;

@EntityView(EventActivity.class)
public interface EventActivityView {
    @IdMapping
    Long getId();

    String getTitle();

    Long getDisplayOrder();

    String getDescription();

    LocalDateTime getCreatedAt();
}
