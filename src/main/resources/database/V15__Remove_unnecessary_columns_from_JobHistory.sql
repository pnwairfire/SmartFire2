ALTER TABLE job_history DROP COLUMN source_id;
ALTER TABLE job_history DROP COLUMN data_start_date;
ALTER TABLE job_history DROP COLUMN data_end_date;
ALTER TABLE job_history RENAME method  TO "type";