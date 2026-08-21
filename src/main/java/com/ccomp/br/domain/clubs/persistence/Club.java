package com.ccomp.br.domain.clubs.persistence;

import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String summary;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

//    @Column(nullable = false)
//    private UUID instructor;

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

//    @Column(name = "participant_limit")
//    private Long participantLimit;

//    public boolean isInstructor(UUID userId) {
//        return userId.equals(instructor);
//    }

    public boolean isPublic() {
        if(publishedAt ==  null) return false;
        return publishedAt.isBefore(LocalDateTime.now());
    }

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClubMember> members = new ArrayList<>();
}
