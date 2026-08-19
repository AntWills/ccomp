package com.ccomp.br.domain.clubs.persistence.members;

import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClubMemberSpec {
    public static Specification<ClubMember> hasClubId(Long clubId) {
        return (root, query, criteriaBuilder) -> {
            if(clubId == null) return criteriaBuilder.conjunction();

            return criteriaBuilder.equal(root.get("clubId"), clubId);
        };
    }

    public static Specification<ClubMember> filterBy(ClubMemberFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.role() != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), filter.role()));
            }

            if (filter.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ClubMember> filterByAndCursor(Long clubId, ClubMemberFilter filter, LocalDateTime cursor) {
        return Specification.where(hasClubId(clubId))
                .and(filterBy(filter))
                .and((root, query, cb) -> {
                    if (cursor == null) {
                        return cb.conjunction();
                    }
                    return cb.lessThan(root.get("joinedAt"), cursor);
                });
    }
}
