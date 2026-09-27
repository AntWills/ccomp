package com.ccomp.br.shared.cache;


import java.util.Map;

import static com.ccomp.br.shared.cache.CacheTier.*;

public final class CacheNames {

    private CacheNames() {}

    // domain/users
    public static final String USERS_BY_ID = "users:by-id";
    public static final String USERS_BY_EMAIL = "users:by-email";

    // domain/clubs
    public static final String CLUBS_BY_ID = "clubs:by-id";
    public static final String CLUB_MEMBERS = "clubs:members";

    // domain/events
    public static final String EVENTS_BY_ID = "events:by-id";
    public static final String EVENTS_BY_SLUG = "events:by-slug";
    public static final String EVENTS_LIST = "events:list";
    public static final String EVENTS_SEARCH_BY = "events:search-by";
    public static final String EVENT_ACTIVITIES = "events:activities";

    // domain/events/editors
    public static final String EVENT_EDITOR_HAS_PERMISSION = "events:editors:has-permission";

    // domain/news
    public static final String NEWS_BY_SLUG = "news:by-slug";

    /**
     * Fonte única de verdade: qual cache pertence a qual tier.
     * Declarado UMA vez, ao lado do nome — não em dois arquivos diferentes.
     */
    public static final Map<String, CacheTier> TIERS = Map.ofEntries(
            Map.entry(USERS_BY_ID, SHORT),
            Map.entry(USERS_BY_EMAIL, SHORT),

            Map.entry(CLUBS_BY_ID, MEDIUM),
            Map.entry(CLUB_MEMBERS, SHORT),

            Map.entry(EVENTS_BY_ID, SHORT),
            Map.entry(EVENTS_BY_SLUG, SHORT),
            Map.entry(EVENTS_LIST, VOLATILE),
            Map.entry(EVENTS_SEARCH_BY, VOLATILE),
            Map.entry(EVENT_ACTIVITIES, SHORT),

            Map.entry(EVENT_EDITOR_HAS_PERMISSION, VOLATILE),

            Map.entry(NEWS_BY_SLUG, LONG)
    );
}
