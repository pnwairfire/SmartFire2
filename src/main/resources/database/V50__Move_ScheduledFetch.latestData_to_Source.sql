ALTER TABLE source ADD COLUMN latest_data date;

UPDATE source set latest_data = data.latest_data from (select source_id, scheduled_fetch.latest_data from scheduled_fetch inner join source on (source.id = scheduled_fetch.source_id)
	where scheduled_fetch.latest_data = (select max(latest_data) from scheduled_fetch where source.id = scheduled_fetch.source_id)) AS data where id = data.source_id;

ALTER TABLE scheduled_fetch DROP COLUMN latest_data;