ALTER TABLE source DROP CONSTRAINT cascade_default_weighting;

DELETE FROM default_weighting;
INSERT INTO default_weighting SELECT id, 0, 0, 0, 0, 0, 0, 0, 0, 0 FROM source;

ALTER TABLE source DROP COLUMN defaultweighting_id;