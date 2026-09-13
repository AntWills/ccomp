-- tb_users
CREATE INDEX idx_users_created_at_id
    ON tb_users (created_at DESC, id DESC);

-- tb_news
ALTER INDEX idx_slug RENAME TO idx_news_slug;

CREATE INDEX idx_news_created_at_id
    ON tb_news (published_at DESC, id DESC);

-- tb_events_editors
CREATE INDEX idx_event_editors_assigned_at_id
    ON tb_event_editors (assigned_at DESC, id DESC);

-- tb_event_enrollments
CREATE INDEX idx_event_enrollment_created_at_id
    ON tb_event_enrollments (created_at DESC, id DESC);

-- tb_clubs
CREATE INDEX idx_clubs_published_at_id
    ON tb_clubs (published_at DESC, id DESC);

CREATE INDEX idx_clubs_created_at_id
    ON tb_clubs (created_at DESC, id DESC);

-- tb_club_members
CREATE INDEX idx_club_members_joined_at_id
    ON tb_club_members (joined_at DESC, id DESC);

-- tb_events

DROP INDEX IF EXISTS idx_events_start_date;

CREATE INDEX idx_events_start_date_id
    ON tb_events (start_date DESC, id DESC);