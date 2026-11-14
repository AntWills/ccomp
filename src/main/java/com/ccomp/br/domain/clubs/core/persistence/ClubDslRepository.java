package com.ccomp.br.domain.clubs.core.persistence;

import com.ccomp.br.domain.clubs.core.dto.ClubCreatedCursor;
import com.ccomp.br.domain.clubs.core.dto.ClubPublishedCursor;
import com.ccomp.br.domain.clubs.core.dto.ClubResponseDTO;
import com.ccomp.br.domain.clubs.core.enums.EnumClubStatus;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.members.enums.EnumClubMemberStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ccomp.br.domain.clubs.core.persistence.QClub.club;
import static com.ccomp.br.domain.clubs.members.persistence.QClubMember.clubMember;

@Repository
public class ClubDslRepository {
    private final JPAQueryFactory queryFactory;

    public ClubDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * Busca pública de clubes publicados usando cursor baseado em (publishedAt, id) decrescentes.
     */
    public List<ClubResponseDTO> findAllPublishedWithCursor(@Nullable ClubPublishedCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        // Compatibilidade com consultas antigas: apenas clubes abertos
        whereClause.and(club.status.eq(EnumClubStatus.OPEN))
                .and(club.publishedAt.isNotNull())
                .and(club.publishedAt.loe(LocalDateTime.now()));

        if (cursor != null && cursor.id() != null && cursor.publishedAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    club.publishedAt,
                    club.id,
                    cursor.publishedAt(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory.select(
                        Projections.constructor(
                                ClubResponseDTO.class,
                                club.id,
                                club.name,
                                club.summary,
                                club.status,
                                club.coverImageUrl,
                                club.content,
                                club.createdAt,
                                club.publishedAt,
                                club.updatedAt
                        )
                )
                .from(club)
                .where(whereClause)
                .orderBy(
                        club.publishedAt.desc(),
                        club.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public List<ClubResponseDTO> findAllOpenWithCursor(@Nullable ClubCreatedCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder().and(club.status.eq(EnumClubStatus.OPEN));

        if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            whereClause.and(Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    club.createdAt, club.id, cursor.createdAt(), cursor.id()));
        }

        return queryFactory.select(Projections.constructor(
                        ClubResponseDTO.class,
                        club.id,
                        club.name,
                        club.summary,
                        club.status,
                        club.coverImageUrl,
                        club.content,
                        club.createdAt,
                        club.publishedAt,
                        club.updatedAt))
                .from(club)
                .where(whereClause)
                .orderBy(club.createdAt.desc(), club.id.desc())
                .limit(limit)
                .fetch();
    }

    /**
     * Busca clubes onde o usuário está envolvido como membro/instrutor, usando cursor baseado em (createdAt, id) decrescentes.
     */
    public List<ClubResponseDTO> findByUserInvolvedWithCursor(
            UUID userId, @Nullable ClubCreatedCursor cursor, int limit) {

        BooleanBuilder whereClause = new BooleanBuilder();

        whereClause.and(clubMember.userId.eq(userId));
        whereClause.and(clubMember.role.in(EnumClubMemberRole.MEMBER, EnumClubMemberRole.INSTRUCTOR));
        whereClause.and(club.status.eq(EnumClubStatus.OPEN)
                .or(club.status.eq(EnumClubStatus.MEMBERS_ONLY)
                        .and(clubMember.status.eq(EnumClubMemberStatus.ACTIVE)))
                .or(club.status.eq(EnumClubStatus.DRAFT)
                        .and(clubMember.role.eq(EnumClubMemberRole.INSTRUCTOR))
                        .and(clubMember.status.eq(EnumClubMemberStatus.ACTIVE))));


        if (cursor != null && cursor.id() != null && cursor.createdAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    club.createdAt,
                    club.id,
                    cursor.createdAt(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory.select(
                        Projections.constructor(
                                ClubResponseDTO.class,
                                club.id,
                                club.name,
                                club.summary,
                                club.status,
                                club.coverImageUrl,
                                club.content,
                                club.createdAt,
                                club.publishedAt,
                                club.updatedAt
                        )
                )
                .from(club)
                .innerJoin(club.members, clubMember)
                .where(whereClause)
                .orderBy(
                        club.createdAt.desc(),
                        club.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}
