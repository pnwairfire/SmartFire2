ALTER TABLE source ADD COLUMN new_data_policy character varying(100);
UPDATE source SET new_data_policy = 'REPLACE' WHERE new_data_policy IS NULL;
ALTER TABLE source ALTER COLUMN new_data_policy SET NOT NULL;
