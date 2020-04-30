ALTER TABLE event ADD COLUMN fire_type character varying(100);
ALTER TABLE fire ADD COLUMN fire_type character varying(100);

UPDATE event SET fire_type = 'NA' WHERE fire_type IS NULL;
UPDATE fire SET fire_type = 'NA' WHERE fire_type IS NULL;

ALTER TABLE event ALTER COLUMN fire_type SET NOT NULL;
ALTER TABLE fire ALTER COLUMN fire_type SET NOT NULL;