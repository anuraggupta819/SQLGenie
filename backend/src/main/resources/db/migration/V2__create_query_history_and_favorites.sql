CREATE TABLE app.query_history (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES app.users (id) ON DELETE CASCADE,
    natural_language_query  TEXT NOT NULL,
    generated_sql           TEXT,
    explanation             TEXT,
    status                  VARCHAR(20) NOT NULL,
    error_message           TEXT,
    execution_time_ms       INTEGER,
    row_count               INTEGER,
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE app.favorite_queries (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES app.users (id) ON DELETE CASCADE,
    name                    VARCHAR(255) NOT NULL,
    natural_language_query  TEXT NOT NULL,
    generated_sql           TEXT NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_favorite_queries_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_query_history_user_created ON app.query_history (user_id, created_at DESC);
CREATE INDEX idx_favorite_queries_user_id ON app.favorite_queries (user_id);
