ALTER TABLE links
    ADD CONSTRAINT ck_links_long_url_len CHECK (char_length(long_url) <= 2048),
    ADD CONSTRAINT ck_links_alias_format CHECK (alias ~ '^[0-9a-zA-Z_-]{5,50}$'),
    ALTER COLUMN expired_at SET DEFAULT (now() + interval '90 days');