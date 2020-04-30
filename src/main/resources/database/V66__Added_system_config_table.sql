CREATE TABLE system_config (
    id integer NOT NULL,
    name varchar(100) UNIQUE NOT NULL,
    config_value text NOT NULL,
    CONSTRAINT system_config_pkey PRIMARY KEY (id)
)
WITH (
  OIDS = FALSE
)
;

CREATE SEQUENCE system_config_seq;