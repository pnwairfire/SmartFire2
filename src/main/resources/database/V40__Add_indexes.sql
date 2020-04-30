CREATE INDEX idx_rawdata_by_clump ON raw_data (clump_id);
CREATE INDEX idx_rawdata_by_source ON raw_data (source_id);

CREATE INDEX idx_clump_by_fire ON clump (fire_id);
CREATE INDEX idx_clump_by_source ON clump (source_id);

CREATE INDEX idx_fire_by_source ON fire (source_id);

CREATE INDEX idx_event_fires_by_event ON event_fires (event_id);
CREATE INDEX idx_event_fires_by_fire ON event_fires (fire_id);

CREATE INDEX idx_event_by_stream ON event (reconciliationstream_id);

CREATE INDEX idx_fire_attribute ON fire_attribute (fire_id);
CREATE INDEX idx_event_attribute ON event_attribute (event_id);
CREATE INDEX idx_source_attribute ON source_attribute (source_id);
CREATE INDEX idx_stream_attribute ON stream_attribute (reconciliation_stream_id);

CREATE INDEX idx_weighting_by_source ON reconciliation_weighting (source_id);
CREATE INDEX idx_weighting_by_stream ON reconciliation_weighting (reconciliationstream_id);

CREATE UNIQUE INDEX idx_source_name_slug ON source (name_slug);
CREATE UNIQUE INDEX idx_reconciliation_stream_name_slug ON reconciliation_stream (name_slug);

CREATE UNIQUE INDEX idx_fire_unique_id ON fire (unique_id);
CREATE UNIQUE INDEX idx_event_unique_id ON event (unique_id);