ALTER TABLE scheduled_fetch ADD COLUMN date_offset integer;

UPDATE scheduled_fetch SET date_offset = 0;

ALTER TABLE scheduled_fetch ALTER COLUMN date_offset SET NOT NULL;
