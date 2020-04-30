ALTER TABLE source DROP CONSTRAINT fkca90681bb2d87b7e;
ALTER TABLE source ADD CONSTRAINT cascade_default_weighting FOREIGN KEY (defaultweighting_id) REFERENCES default_weighting (id) MATCH FULL ON UPDATE NO ACTION ON DELETE CASCADE;
