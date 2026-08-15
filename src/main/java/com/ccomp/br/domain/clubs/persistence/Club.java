package com.ccomp.br.domain.clubs.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_clubs", indexes = {
//        @Index(name = "idx_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {
    @Id
    private Long id;
    @Column(nullable = false)
    private String name;

    private String summary;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(nullable = false)
    private UUID instructor;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(columnDefinition = "text")
    private String content;

    public boolean isInstructor(UUID userId) {
        return userId.equals(instructor);
    }
}
