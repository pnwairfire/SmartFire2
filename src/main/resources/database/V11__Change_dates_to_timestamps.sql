ALTER TABLE clump ALTER end_date TYPE timestamp without time zone;
ALTER TABLE clump ALTER start_date TYPE timestamp without time zone;

ALTER TABLE event ALTER end_date TYPE timestamp without time zone;
ALTER TABLE event ALTER start_date TYPE timestamp without time zone;

ALTER TABLE scheduled_fetch ALTER last_fetch TYPE timestamp without time zone;
ALTER TABLE scheduled_fetch ALTER latest_data TYPE timestamp without time zone;

ALTER TABLE summary_data_layer ALTER end_date TYPE timestamp without time zone;
ALTER TABLE summary_data_layer ALTER start_date TYPE timestamp without time zone;