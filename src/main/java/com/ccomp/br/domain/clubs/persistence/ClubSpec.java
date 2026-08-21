package com.ccomp.br.domain.clubs.persistence;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public class ClubSpec {
    public static Specification<Club> publishedBeforeCursor(LocalDateTime cursor) {
        return (root, query, cb) -> {
            if(cursor == null)
                return cb.lessThanOrEqualTo(root.get("publishedAt"), LocalDateTime.now());
            return cb.lessThan(root.get("publishedAt"), cursor);
        };
    }

    public static Specification<Club> createdBeforeCursor(LocalDateTime cursor) {
        return (root, query, cb) -> {
            if (cursor == null) {
                return cb.conjunction(); // Sem restrição de data na primeira página
            }
            return cb.lessThan(root.get("createdAt"), cursor);
        };
    }

    public static Specification<Club> byInstructor(UUID instructor) {
        return (root, query, cb) -> {
            Join<Club, ClubMember> members = root.join("members");
            return cb.and(
                cb.equal(members.get("userId"), instructor),
                cb.equal(members.get("role"), EnumClubMemberRole.INSTRUCTOR)
            );
        };
    }

    public static Specification<Club> buildSpecByCursor(LocalDateTime cursor) {
        return Specification.where(publishedBeforeCursor(cursor));
//                .and(NewsSpecs.isFeatured(filter.featured()));
    }

    public static Specification<Club> buildSpecByInstructorAndCursor(UUID instructor, LocalDateTime cursor) {
        return Specification.where(byInstructor(instructor))
                .and(createdBeforeCursor(cursor));
    }
}
