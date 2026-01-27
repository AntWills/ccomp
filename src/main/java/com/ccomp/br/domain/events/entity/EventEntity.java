package com.ccomp.br.domain.events.entity;

import com.ccomp.br.shared.exceptions.DomainException;

import java.time.LocalDate;
import java.util.UUID;

public class EventEntity {
    private Long id;
    private String name;
    private LocalDate start;
    private LocalDate end;
    private UUID ownerId;

    public EventEntity(String name, UUID ownerId) {
        this.name = name;
        this.ownerId = ownerId;
    }

    public EventEntity(String name, UUID ownerId, LocalDate start, LocalDate end) {
        this.name = name;
        this.ownerId = ownerId;
        this.start = start;
        this.end = end;

        verifyDate();
    }

    public boolean isOpen() {
        LocalDate now = LocalDate.now();
        return now.isAfter(start) && now.isBefore(end);
    }

    public boolean isClosed() {
        return !this.isOpen();
    }

    public boolean isOwner(UUID userId){
        return userId.equals(ownerId);
    }

    private void verifyDate() {
        if (start == null || end == null) return;

        if (start.isAfter(end)) throw new DomainException("The event cannot start after the event ends.");
    }

    public void setStart(LocalDate start) {
        this.start = start;
        verifyDate();
    }

    public void setEnd(LocalDate end) {
        this.end = end;
        verifyDate();
    }
}
