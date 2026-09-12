package com.ccomp.br.domain.events.enrollments.util;

import com.ccomp.br.domain.events.enrollments.dto.EnrollmentListItem;
import com.ccomp.br.domain.events.enrollments.persistence.Enrollment;
import com.ccomp.br.shared.dto.UserSummaryView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EnrollmentMapper {

    /**
     * Mapeia uma entidade de inscrição juntamente com a visão resumida do usuário.
     */
    public EnrollmentListItem toListItem(Enrollment enrollment, UserSummaryView user) {
        return new EnrollmentListItem(
                enrollment.getId(),
                enrollment.getStatus(),
                enrollment.getCreatedAt(),
                user
        );
    }

    /**
     * Mapeia uma lista de inscrições efetuando a associação em memória via Map.
     */
    public List<EnrollmentListItem> toListItemList(List<Enrollment> enrollments, List<UserSummaryView> userSummaries) {
        if (enrollments == null || enrollments.isEmpty()) {
            return List.of();
        }

        // Indexa os usuários pelo ID para busca O(1)
        Map<UUID, UserSummaryView> userMap = userSummaries.stream()
                .collect(Collectors.toMap(
                        UserSummaryView::getId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        return enrollments.stream()
                .map(enrollment -> toListItem(enrollment, userMap.get(enrollment.getUserId())))
                .toList();
    }
}