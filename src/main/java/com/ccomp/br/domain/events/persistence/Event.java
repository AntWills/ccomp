package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.enrollments.Enrollment;
import com.ccomp.br.shared.exceptions.DomainException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table(name = "tb_events")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "event")
    private Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(mappedBy = "event")
    private Set<EventEditor> editors;

    @OneToMany(mappedBy = "event")
    private List<EventActivity> activities;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    public Event(String name, UUID ownerId) {
        this.name = name;
        this.ownerId = ownerId;
    }

    private void verifyDate() {
        if (startDate == null || endDate == null) return;

        if (startDate.isAfter(endDate)) throw new DomainException("The event cannot start after the event ends.");
    }

    public boolean isOpen() {
        if (startDate == null || endDate == null) return false;

        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate) && now.isBefore(endDate);
    }

    public boolean isClosed() {
        return !this.isOpen();
    }

    public boolean isOwner(UUID userId){
        return ownerId.equals(userId);
    }

    public void setStart(LocalDateTime start) {
        this.startDate = start;
        verifyDate();
    }

    public void setEnd(LocalDateTime end) {
        this.endDate = end;
        verifyDate();
    }
}
