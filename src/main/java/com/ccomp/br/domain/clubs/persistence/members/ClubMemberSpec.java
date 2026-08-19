package com.ccomp.br.domain.clubs.persistence.members;

import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClubMemberSpec {

    public static Specification<ClubMember> filterBy(ClubMemberFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getRole() != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), filter.getRole()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ClubMember> filterByAndCursor(ClubMemberFilter filter, LocalDateTime cursor) {
        return Specification.where(filterBy(filter))
                .and((root, query, cb) -> {
                    if (cursor == null) {
                        return cb.conjunction();
                    }
                    return cb.lessThan(root.get("joinedAt"), cursor);
                });
    }
}
