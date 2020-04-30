create table fetch_attribute (
        id int4 not null,
        attr_value varchar(100) not null,
        name varchar(100) not null,
        fetch_id int8,
        primary key (id)
    );

ALTER TABLE fetch_attribute ADD CONSTRAINT fetch_attribute_reference FOREIGN KEY (fetch_id) REFERENCES scheduled_fetch (id) ON UPDATE NO ACTION ON DELETE NO ACTION;
