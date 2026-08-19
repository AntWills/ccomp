package com.ccomp.br.domain.clubs.persistence.members;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_club_members",
        indexes = {
            @Index(name = "idx_club_members_user_id", columnList = "user_id"),
            @Index(name = "idx_club_members_club_id", columnList = "club_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_club_members_club_user",
                        columnNames = {"club_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(25)")
    private EnumClubMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(25)")
    private EnumClubMemberStatus status;

//    @Column(name = "edition")
//    private String edition;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public void activate() {
        this.status = EnumClubMemberStatus.ACTIVE;
        this.leftAt = null;
    }

    public void deactivate() {
        this.status = EnumClubMemberStatus.INACTIVE;
        this.leftAt = LocalDateTime.now();
    }

    public boolean isStillLinked() {
        return this.status == EnumClubMemberStatus.ACTIVE;
    }
}
