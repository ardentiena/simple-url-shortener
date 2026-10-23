CREATE TABLE links (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    alias VARCHAR(50) NOT NULL,
    long_url TEXT NOT NULL,
    expired_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_links PRIMARY KEY (id),
    CONSTRAINT uq_links_alias UNIQUE (alias)
);