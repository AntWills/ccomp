package com.ccomp.br.domain.events.core.application;

import com.ccomp.br.domain.events.activities.materials.persistence.ActivityMaterialRepository;
import com.ccomp.br.domain.events.activities.persistence.EventActivityRepository;
import com.ccomp.br.domain.events.certificate.persistence.EventCoverTemplateRepository;
import com.ccomp.br.domain.events.certificate.persistence.ActivityCertificateTemplateRepository;
import com.ccomp.br.domain.events.core.persistence.Event;
import com.ccomp.br.domain.events.guests.persistence.EventGuestRepository;
import com.ccomp.br.domain.events.materials.persistence.EventMaterialRepository;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class EventStorageFileKeyCollector {
    private final EventMaterialRepository eventMaterialRepository;
    private final EventActivityRepository activityRepository;
    private final ActivityMaterialRepository activityMaterialRepository;
    private final EventGuestRepository eventGuestRepository;
    private final EventCoverTemplateRepository coverTemplateRepository;
    private final ActivityCertificateTemplateRepository activityCertificateTemplateRepository;

    @Autowired
    public EventStorageFileKeyCollector(EventMaterialRepository eventMaterialRepository, EventActivityRepository activityRepository, ActivityMaterialRepository activityMaterialRepository, EventGuestRepository eventGuestRepository, EventCoverTemplateRepository coverTemplateRepository, ActivityCertificateTemplateRepository activityCertificateTemplateRepository) {
        this.eventMaterialRepository = eventMaterialRepository;
        this.activityRepository = activityRepository;
        this.activityMaterialRepository = activityMaterialRepository;
        this.eventGuestRepository = eventGuestRepository;
        this.coverTemplateRepository = coverTemplateRepository;
        this.activityCertificateTemplateRepository = activityCertificateTemplateRepository;
    }

    public EventStorageFileKeyCollector(EventMaterialRepository eventMaterialRepository,
                                        EventActivityRepository activityRepository,
                                        ActivityMaterialRepository activityMaterialRepository,
                                        EventGuestRepository eventGuestRepository,
                                        EventCoverTemplateRepository coverTemplateRepository) {
        this(eventMaterialRepository, activityRepository, activityMaterialRepository, eventGuestRepository,
                coverTemplateRepository, null);
    }

    public List<String> collect(Event event) {
        Long eventId = event.getId();
        List<String> fileKeys = new ArrayList<>();

        Optional.ofNullable(event.getCoverImageKey()).ifPresent(fileKeys::add);
        eventMaterialRepository.findAllByEventId(eventId).stream()
                .map(material -> material.getFileKey()).filter(Objects::nonNull).forEach(fileKeys::add);

        List<Long> activityIds = activityRepository.findAllByEventId(eventId).stream()
                .map(activity -> activity.getId()).toList();
        if (!activityIds.isEmpty()) {
            activityMaterialRepository.findAllByActivityIdIn(activityIds).stream()
                    .map(material -> material.getFileKey()).filter(Objects::nonNull).forEach(fileKeys::add);
        }

        eventGuestRepository.findAllByEventId(eventId).stream()
                .map(guest -> guest.getImageKey()).filter(Objects::nonNull).forEach(fileKeys::add);
        coverTemplateRepository.findByEventId(eventId)
                .map(template -> template.getCoverTemplateKey()).ifPresent(fileKeys::add);
        if (activityCertificateTemplateRepository != null && !activityIds.isEmpty()) {
            activityCertificateTemplateRepository.findAllByActivityIdIn(activityIds).stream()
                    .map(template -> template.getCoverTemplateKey()).filter(Objects::nonNull).forEach(fileKeys::add);
        }

        return fileKeys.stream().distinct().toList();
    }
}
