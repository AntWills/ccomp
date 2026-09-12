package com.ccomp.br.domain.news.persistence;

import com.ccomp.br.domain.news.dto.NewsCursor;
import com.ccomp.br.domain.news.dto.NewsSearchFilter;
import com.ccomp.br.domain.news.dto.NewsItem;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ccomp.br.domain.news.persistence.QNews.news;

@Repository
public class NewsDslRepository {
    private final JPAQueryFactory queryFactory;

    public NewsDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<NewsItem> findByCursor(NewsSearchFilter filter, @Nullable NewsCursor cursor, int limit) {
        BooleanBuilder whereClause = new BooleanBuilder();

        filter.featuredOpt().ifPresent(f ->
                whereClause.and(news.featured.eq(f))
        );

        if(cursor != null && cursor.id() != null && cursor.publishedAt() != null) {
            BooleanExpression cursorCondition = Expressions.booleanTemplate(
                    "( {0}, {1} ) < ( {2}, {3} )",
                    news.publishedAt,
                    news.id,
                    cursor.publishedAt(),
                    cursor.id()
            );
            whereClause.and(cursorCondition);
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                NewsItem.class,
                                news.id,
                                news.title,
                                news.summary,
                                news.slug,
                                news.coverImageUrl,
                                news.publishedAt,
                                news.featured
                        )
                )
                .from(news)
                .where(whereClause)
                .orderBy(
                        news.publishedAt.desc(),
                        news.id.desc()
                )
                .limit(limit)
                .fetch();
    }
}
