ALTER TABLE source ADD COLUMN ingest_method character varying(100);

UPDATE source SET ingest_method = 'smartfire.func.fetch.DefaultUploadIngestMethod';