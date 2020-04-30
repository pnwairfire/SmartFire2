ALTER TABLE raw_data ADD COLUMN clump_id integer;
ALTER TABLE raw_data ADD CONSTRAINT fk_raw_data_clump FOREIGN KEY (clump_id) REFERENCES clump (id) MATCH FULL ON UPDATE NO ACTION ON DELETE CASCADE;

UPDATE raw_data
SET clump_id = cd.clump_id
FROM clump_data cd 
WHERE cd.raw_data_id = raw_data.id
;

DROP TABLE clump_data;

ALTER TABLE clump ADD COLUMN fire_id integer;
ALTER TABLE clump ADD CONSTRAINT fk_clump_fire FOREIGN KEY (fire_id) REFERENCES fire (id) MATCH FULL ON UPDATE NO ACTION ON DELETE CASCADE;

UPDATE clump
SET fire_id = fc.fire_id
FROM fire_clumps fc 
WHERE fc.clump_id = clump.id
;

DROP TABLE fire_clumps;