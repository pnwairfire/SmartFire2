UPDATE source SET name_slug = id WHERE name_slug IS NULL;

ALTER TABLE source
   ALTER COLUMN name_slug SET NOT NULL;
ALTER TABLE source ADD CONSTRAINT unique_name_slug UNIQUE (name_slug);
