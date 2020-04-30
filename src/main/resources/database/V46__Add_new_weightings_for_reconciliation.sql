ALTER TABLE default_weighting DROP COLUMN end_time_weight;
ALTER TABLE default_weighting DROP COLUMN start_time_weight;
ALTER TABLE default_weighting ADD COLUMN location_uncertainty double precision NOT NULL DEFAULT 0.0;
ALTER TABLE default_weighting ADD COLUMN start_date_uncertainty integer NOT NULL DEFAULT 0;
ALTER TABLE default_weighting ADD COLUMN end_date_uncertainty integer NOT NULL DEFAULT 0;
ALTER TABLE default_weighting ADD COLUMN name_weight double precision NOT NULL DEFAULT 0.0;

ALTER TABLE reconciliation_weighting DROP COLUMN end_time_weight;
ALTER TABLE reconciliation_weighting DROP COLUMN start_time_weight;
ALTER TABLE reconciliation_weighting ADD COLUMN location_uncertainty double precision NOT NULL DEFAULT 0.0;
ALTER TABLE reconciliation_weighting ADD COLUMN start_date_uncertainty integer NOT NULL DEFAULT 0;
ALTER TABLE reconciliation_weighting ADD COLUMN end_date_uncertainty integer NOT NULL DEFAULT 0;
ALTER TABLE reconciliation_weighting ADD COLUMN name_weight double precision NOT NULL DEFAULT 0.0;