package com.ccomp.br.domain.events.persistence;

import com.ccomp.br.domain.events.enums.EnumEventCategory;
import com.ccomp.br.domain.events.persistence.activities.EventActivity;
import com.ccomp.br.domain.events.persistence.editors.EventEditor;
import com.ccomp.br.domain.events.persistence.enrollments.Enrollment;
import com.ccomp.br.shared.exceptions.DomainException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table(name = "tb_events", indexes = {
        @Index(name = "idx_events_slug", columnList = "slug"),
        @Index(name = "idx_category_start_id", columnList = "category, start_date, id")
})
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
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

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

    @Size(max = 1000, message = "O máximo são 1000 letras.")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", columnDefinition = "varchar(25)", nullable = false)
    private EnumEventCategory category;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Event(String title, UUID ownerId) {
        this.title = title;
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
