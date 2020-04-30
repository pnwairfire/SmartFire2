ALTER TABLE clump ADD COLUMN source_id integer NOT NULL;

alter table clump add constraint FK1DFCC12340AB7658 foreign key (source_id) references source;