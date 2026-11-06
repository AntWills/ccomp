-- Match is the indexable candidate filter. Its threshold is configured per
-- transaction through pg_trgm.word_similarity_threshold.
CREATE OR REPLACE FUNCTION text_search_match(
    search_term text, candidate text, min_similarity double precision)
RETURNS boolean
LANGUAGE sql
STABLE
PARALLEL SAFE
AS $$
    SELECT search_term OPERATOR(public.<%) candidate
       AND word_similarity(search_term, candidate) >= min_similarity
$$;

-- Score is used for ranking after candidate filtering. GIN does not index this
-- calculated score; it remains a per-row calculation over the matching set.
CREATE OR REPLACE FUNCTION text_search_score(search_term text, candidate text)
RETURNS double precision
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT word_similarity(search_term, candidate) + similarity(search_term, candidate)
$$;
