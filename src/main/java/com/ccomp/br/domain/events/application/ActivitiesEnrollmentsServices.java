package com.ccomp.br.domain.events.application;

import com.ccomp.br.domain.events.persistence.activities.EventActivityRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ActivitiesEnrollmentsServices {
    public final EventActivityRepository activityRepository;

    public ActivitiesEnrollmentsServices(EventActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public void inscribe(UUID userId) {

    }
}
