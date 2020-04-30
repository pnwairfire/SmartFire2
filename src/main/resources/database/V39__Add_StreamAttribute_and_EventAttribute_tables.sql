CREATE TABLE stream_attribute (
        id int4 not null,
        reconciliation_stream_id int8,
        attr_name varchar(100) not null,        
        attr_value varchar(100) not null,
        PRIMARY KEY (id)
    );

ALTER TABLE stream_attribute 
ADD CONSTRAINT fk_reconciliation_stream_attribute
FOREIGN KEY (reconciliation_stream_id) 
REFERENCES reconciliation_stream (id) 
ON UPDATE CASCADE 
ON DELETE CASCADE;

CREATE TABLE event_attribute (
        id int8 not null,
        event_id int8,
        attr_name varchar(100) not null,        
        attr_value varchar(100) not null,
        PRIMARY KEY (id)
    );

ALTER TABLE event_attribute 
ADD CONSTRAINT fk_event_attribute
FOREIGN KEY (event_id) 
REFERENCES event (id)
ON UPDATE CASCADE 
ON DELETE CASCADE;