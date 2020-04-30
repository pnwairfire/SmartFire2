ALTER TABLE job_history ADD COLUMN final_status character varying(100);

UPDATE job_history SET final_status = 'Successfully Completed' WHERE final_status IS NULL;

ALTER TABLE job_history ALTER COLUMN final_status SET NOT NULL;