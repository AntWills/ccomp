package com.ccomp.br.shared.utils;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class TextSearchDSL {
    private TextSearchDSL() {}

    public static BooleanExpression match(Expression<String> term, Expression<String> field, double minSimilarity) {
        return Expressions.booleanTemplate(
                "function('text_search_match', {0}, {1}, {2}) = true",
                term, field, Expressions.constant(minSimilarity));
    }

    public static NumberExpression<Double> score(Expression<String> term, Expression<String> field) {
        return Expressions.numberTemplate(Double.class,
                "function('text_search_score', {0}, {1})", term, field);
    }

    /** Exact threshold check used after the indexable candidate filter. */
    public static BooleanExpression wordSimilarityAtLeast(
            Expression<String> term, Expression<String> field, double threshold) {
        var similarity = Expressions.numberTemplate(Double.class,
                "cast(function('word_similarity', {0}, {1}) as double)", term, field);
        return similarity.goe(threshold);
    }

    /** Exact threshold check in any of the fields. */
    @SafeVarargs
    public static BooleanExpression wordSimilarityAtLeastAny(
            String term, double threshold, Expression<String>... fields) {
        requireFields(fields);
        var termExpression = Expressions.constant(term);
        BooleanExpression result = null;
        for (var field : fields) {
            var fieldMatch = wordSimilarityAtLeast(termExpression, field, threshold);
            result = result == null ? fieldMatch : result.or(fieldMatch);
        }
        return result;
    }

    /** Indexable candidate in any of the fields. */
    @SafeVarargs
    public static BooleanExpression matchAny(String term, double minSimilarity, Expression<String>... fields) {
        requireFields(fields);
        var t = Expressions.constant(term);
        BooleanExpression result = null;
        for (var f : fields) {
            var m = match(t, f, minSimilarity);
            result = result == null ? m : result.or(m);
        }
        return result;
    }

    /** Highest score among the fields. */
    @SafeVarargs
    public static NumberExpression<Double> bestScore(String term, Expression<String>... fields) {
        requireFields(fields);
        var termExpression = Expressions.constant(term);
        Expression<?>[] scores = Arrays.stream(fields)
                .map(field -> score(termExpression, field))
                .toArray(Expression<?>[]::new);

        if (scores.length == 1) {
            return (NumberExpression<Double>) scores[0];
        }

        String arguments = IntStream.range(0, scores.length)
                .mapToObj(index -> "{" + index + "}")
                .collect(Collectors.joining(", "));
        return Expressions.numberTemplate(Double.class,
                "greatest(" + arguments + ")", (Object[]) scores);
    }

    private static void requireFields(Expression<String>[] fields) {
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException("At least one text field is required.");
        }
    }
}
