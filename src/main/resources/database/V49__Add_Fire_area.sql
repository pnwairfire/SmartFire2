ALTER TABLE fire ADD COLUMN area float8;

UPDATE fire
SET area = (
        SELECT ST_area(ST_union(c.shape)) AS area
        FROM Clump c
        WHERE c.fire_id = fire.id
    )
WHERE area IS NULL;

ALTER TABLE fire ALTER COLUMN area SET NOT NULL;
