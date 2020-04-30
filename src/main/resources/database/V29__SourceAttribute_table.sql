create table source_attribute (
        id int4 not null,
        attr_value varchar(100) not null,
        name varchar(100) not null,
        source_id int8,
        primary key (id)
    );

ALTER TABLE source_attribute ADD CONSTRAINT source_attribute_reference FOREIGN KEY (source_id) REFERENCES source (id) ON UPDATE NO ACTION ON DELETE NO ACTION;
