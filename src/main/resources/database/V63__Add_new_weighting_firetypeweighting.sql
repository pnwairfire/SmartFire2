ALTER TABLE default_weighting ADD COLUMN type_weight double precision NOT NULL DEFAULT 0.0;
ALTER TABLE reconciliation_weighting ADD COLUMN type_weight double precision NOT NULL DEFAULT 0.0;