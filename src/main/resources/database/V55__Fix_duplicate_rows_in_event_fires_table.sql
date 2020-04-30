-- Using the magic Postgres "ctid" because we don't have a PK
DELETE FROM event_fires
WHERE ctid NOT IN (
    SELECT max(ctid)
    FROM event_fires
    GROUP BY fire_id, event_id
);

-- Add a composite PK so this doesn't happen again
ALTER TABLE event_fires ADD CONSTRAINT pk_event_fires PRIMARY KEY (fire_id, event_id);

-- And, while we're at it, drop and re-create the foreign keys so we can give them reasonable names
ALTER TABLE event_fires DROP CONSTRAINT fk1a13cb18ebde83f8;
ALTER TABLE event_fires DROP CONSTRAINT fk1a13cb18c212ad3c;

ALTER TABLE event_fires 
ADD CONSTRAINT fk_event_fires_event 
FOREIGN KEY (event_id) 
REFERENCES event (id) 
MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
  
ALTER TABLE event_fires 
ADD CONSTRAINT fk_event_fires_fire
FOREIGN KEY (fire_id)
REFERENCES fire (id) 
MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;