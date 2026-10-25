DELETE FROM links WHERE alias LIKE 'bench%';

INSERT INTO links (alias, long_url, expired_at)
SELECT
    'bench' || lpad(sequence_number::text, 6, '0'),
    'https://example.com/benchmark/page/' || sequence_number,
    now() + interval '90 days'
FROM generate_series(1, 100000) AS sequence_number;

ANALYZE links;
