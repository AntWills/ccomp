package com.ccomp.br.domain.clubs.persistence;

import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
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

    public static Specification<Club> involvedUser(UUID userId, EnumClubMemberRole role) {
        return (root, query, cb) -> {
            Join<Club, ClubMember> member = root.join("members");

            return cb.and(
                    isMemberUser(cb, member, userId),
                    isMemberActive(cb, member),
                    hasMemberRole(cb, member, role)
            );
        };
    }

    public static Specification<Club> buildSpecByCursor(LocalDateTime cursor) {
        return Specification.where(publishedBeforeCursor(cursor));
//                .and(NewsSpecs.isFeatured(filter.featured()));
    }

    public static Specification<Club> buildSpecByInvolvedUserAndCursor(UUID userId, EnumClubMemberRole role, LocalDateTime cursor) {
        return Specification.where(involvedUser(userId, role))
                .and(createdBeforeCursor(cursor));
    }

    private static Predicate isMemberUser(CriteriaBuilder cb, Join<Club, ClubMember> member, UUID userId) {
        return cb.equal(member.get("userId"), userId);
    }

    private static Predicate isMemberActive(CriteriaBuilder cb, Join<Club, ClubMember> member) {
        return cb.notEqual(member.get("status"), EnumClubMemberStatus.CANCELLED);
    }

    private static Predicate hasMemberRole(CriteriaBuilder cb, Join<Club, ClubMember> member, EnumClubMemberRole role) {
        if (role == null) {
            return cb.conjunction(); // Neutro: não aplica filtro se a role for nula
        }
        return cb.equal(member.get("role"), role);
    }
}
