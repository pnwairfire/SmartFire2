ALTER TABLE reconciliation_stream ADD COLUMN name_slug character varying(100) NOT NULL;
ALTER TABLE reconciliation_stream ADD CONSTRAINT unique_stream_name_slug UNIQUE (name_slug);