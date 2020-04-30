ALTER TABLE source ADD COLUMN fire_name_field character varying(100);

UPDATE source
SET fire_name_field = 'incident name'
WHERE assoc_method = 'smartfire.func.assoc.ICS209AssociationMethod'