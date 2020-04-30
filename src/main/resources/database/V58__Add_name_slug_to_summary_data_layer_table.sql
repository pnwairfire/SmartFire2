ALTER TABLE summary_data_layer ADD COLUMN name_slug character varying(100) NOT NULL;
ALTER TABLE summary_data_layer ADD CONSTRAINT unique_layer_name_slug UNIQUE (name_slug);