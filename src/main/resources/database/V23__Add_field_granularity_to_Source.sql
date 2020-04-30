ALTER TABLE source ADD COLUMN granularity character varying(100);
UPDATE source SET granularity = '1Days' WHERE granularity IS NULL;
ALTER TABLE source ALTER COLUMN granularity SET NOT NULL;