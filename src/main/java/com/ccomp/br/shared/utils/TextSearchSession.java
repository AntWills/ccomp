package com.ccomp.br.shared.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class TextSearchSession {
    private static final double CANDIDATE_MARGIN = 1e-6;

    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public void useWordSimilarityThreshold(double minSimilarity) {
        var candidate = BigDecimal.valueOf(Math.max(0, minSimilarity - CANDIDATE_MARGIN))
                .toPlainString();
        em.createNativeQuery("select set_config('pg_trgm.word_similarity_threshold', :v, true)")
                .setParameter("v", candidate)
                .getSingleResult();
    }
}